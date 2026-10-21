package org.scraper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.entity.Categoria;
import org.entity.Medidas;
import org.entity.Mercados;
import org.entity.Precos;
import org.entity.Produtos;
import org.entity.VinculosProdutos;
import org.jboss.logging.Logger;
import org.repository.CategoriaRepository;
import org.repository.MedidasRepository;
import org.repository.MercadoRepository;
import org.repository.PrecosRepository;
import org.repository.ProdutosRepository;
import org.scraper.dto.ScrapedProdutoDTO;
import org.scraper.juncao.CasadorPorNome;

import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProdutoPrecoSyncService {

    private static final Logger LOG = Logger.getLogger(ProdutoPrecoSyncService.class);
    private static final String CATEGORIA_PADRAO = "Outros";

    @Inject MercadoRepository mercadoRepository;
    @Inject ProdutosRepository produtosRepository;
    @Inject CategoriaRepository categoriaRepository;
    @Inject MedidasRepository medidasRepository;
    @Inject PrecosRepository precosRepository;

    @Transactional
    @TransactionConfiguration(timeout = 1800) // catálogos grandes (ex.: ~16 mil produtos) passam do timeout padrão de 60s
    public void sincronizar(String nomeMercado, List<ScrapedProdutoDTO> itens) {
        Mercados mercado = mercadoRepository.findByName(nomeMercado);
        if (mercado == null) {
            LOG.warnf("Mercado '%s' não encontrado no banco - pulando sincronização", nomeMercado);
            return;
        }

        int criados = 0, atualizados = 0, ignorados = 0, ligadosPorNome = 0;

        // cache local para não consultar o banco a cada item da mesma categoria/medida
        Map<String, Categoria> categorias = new HashMap<>();
        Map<String, Medidas> medidas = new HashMap<>();

        // produtos e preços carregados de uma vez só: consultar o banco item a item dentro da
        // transação força um flush a cada consulta e deixa a sincronização quadrática (muito lenta)
        Map<String, Produtos> produtos = new HashMap<>();
        Map<String, Produtos> porCodigoBarras = new HashMap<>();
        for (Produtos p : produtosRepository.listAllComMedida()) {
            produtos.putIfAbsent(chave(p.nome, p.getMedidas().id), p);
            String codigo = normalizarCodigoBarras(p.getCodigoBarras());
            if (codigo != null) porCodigoBarras.putIfAbsent(codigo, p);
        }
        Map<Long, Precos> precos = new HashMap<>();
        for (Precos p : precosRepository.listByMercado(mercado)) {
            precos.putIfAbsent(p.getProdutos().id, p);
        }

        // ligações por nome já feitas antes: o item vai direto para o produto ligado (não é recriado)
        Map<String, Produtos> vinculos = new HashMap<>();
        for (VinculosProdutos v : VinculosProdutos.<VinculosProdutos>list("mercado", mercado)) {
            vinculos.put(v.chave, v.produto);
        }

        // em quais mercados cada produto tem preço; produtos de OUTROS mercados são candidatos à ligação por nome
        Map<Long, Set<Long>> mercadosDoProduto = precosRepository.mercadosPorProduto();
        Map<Long, Produtos> produtosPorId = new HashMap<>();
        produtos.values().forEach(p -> produtosPorId.put(p.id, p));
        CasadorPorNome<Produtos> casador = new CasadorPorNome<>();
        mercadosDoProduto.forEach((produtoId, mercadosIds) -> {
            Produtos p = produtosPorId.get(produtoId);
            if (p != null && !mercadosIds.contains(mercado.getId())) casador.adicionar(p, p.nome, p.getMarca());
        });
        List<Produtos> semPreco = new ArrayList<>(); // cadastros antigos que ficaram sem preço depois de ligados

        for (ScrapedProdutoDTO item : itens) {
            if (item.nome == null || item.nome.isBlank() || item.preco == null) {
                ignorados++;
                continue;
            }

            // a categoria vem do departamento do site; se ainda não existir no banco, é cadastrada
            String nomeCategoria = (item.categoria == null || item.categoria.isBlank()) ? CATEGORIA_PADRAO : item.categoria.trim();
            String nomeMedida = (item.medida == null || item.medida.isBlank()) ? MedidaParser.PADRAO : item.medida.trim();

            Categoria categoria = categorias.computeIfAbsent(nomeCategoria.toLowerCase(), k -> categoriaRepository.findOrCreate(nomeCategoria));
            Medidas medida = medidas.computeIfAbsent(nomeMedida.toLowerCase(), k -> medidasRepository.findOrCreate(nomeMedida));

            // o código de barras (opcional) identifica o mesmo produto em mercados que o nomeiam de
            // formas diferentes; sem ele, vale nome + medida
            String codigo = normalizarCodigoBarras(item.codigoBarras);
            Produtos produto = codigo != null ? porCodigoBarras.get(codigo) : null;
            if (produto == null && codigo == null) {
                produto = vinculos.get(chave(item.nome, medida.id));
            }
            if (produto == null) {
                produto = produtos.get(chave(item.nome, medida.id));
                String codigoDoBanco = produto == null ? null : normalizarCodigoBarras(produto.getCodigoBarras());
                if (codigoDoBanco != null && codigo != null && !codigoDoBanco.equals(codigo)) {
                    produto = null; // mesmo nome, mas código de barras diferente: é outro produto
                }

                // sem código de barras, o nome do site pode ser diferente do cadastrado por outro mercado:
                // tenta ligar ao mesmo produto de outro mercado. Só quando não há produto, ou quando o
                // existente é só deste mercado e não tem código de barras (cadastrado por uma coleta daqui)
                boolean soDesteMercado = produto != null && produto.getCodigoBarras() == null
                        && Set.of(mercado.getId()).equals(mercadosDoProduto.get(produto.id));
                if (codigo == null && (produto == null || soDesteMercado)) {
                    Produtos ligado = casador.encontrar(item.nome, item.marca);
                    if (ligado != null) {
                        if (produto != null) moverPreco(produto, ligado, precos, semPreco);
                        produto = ligado;
                        String chaveItem = chave(item.nome, medida.id);
                        VinculosProdutos.persist(new VinculosProdutos(mercado, chaveItem, ligado));
                        vinculos.put(chaveItem, ligado);
                        ligadosPorNome++;
                    }
                }
            }

            if (produto == null) {
                produto = new Produtos(item.nome, medida, categoria);
                produto.medida = item.quantidade;
                produto.setMarca(item.marca);
                produto.setCodigoBarras(codigo);
                produtosRepository.persist(produto);
                produtos.putIfAbsent(chave(item.nome, medida.id), produto);
                criados++;
            } else {
                if (produto.getCategoria() == null) {
                    produto.setCategoria(categoria); // completa produtos antigos que estavam sem categoria
                }
                if (codigo != null && produto.getCodigoBarras() == null) {
                    produto.setCodigoBarras(codigo); // completa produtos cadastrados antes sem código de barras
                }
            }
            if (codigo != null) porCodigoBarras.putIfAbsent(codigo, produto);
            // o produto passou a ter preço deste mercado: não pode mais ser ligado a outro item daqui
            mercadosDoProduto.computeIfAbsent(produto.id, k -> new HashSet<>()).add(mercado.getId());
            casador.remover(produto);

            // entidades carregadas na transação são gravadas no commit; só o que mudou gera UPDATE
            Precos precoExistente = precos.get(produto.id);
            if (precoExistente != null) {
                precoExistente.setPreco(item.preco);
                precoExistente.setUltimaAtualizacao(LocalDate.now());
            } else {
                Precos novo = new Precos(item.preco, mercado, produto);
                precosRepository.persist(novo);
                precos.put(produto.id, novo);
            }
            atualizados++;
        }

        // o preço foi para o produto ligado; o cadastro antigo, sem nenhum preço, apareceria nas buscas como "sem preço"
        for (Produtos vazio : semPreco) {
            VinculosProdutos.delete("produto", vazio);
            produtosRepository.delete(vazio);
        }

        LOG.infof("[%s] sincronização concluída: %d produtos criados, %d preços atualizados, %d ligados por nome "
                + "a produtos de outros mercados (%d cadastros duplicados removidos), %d ignorados",
                nomeMercado, criados, atualizados, ligadosPorNome, semPreco.size(), ignorados);
    }

    /** Leva o preço deste mercado do cadastro antigo para o produto ligado; o antigo fica para remoção. */
    private void moverPreco(Produtos antigo, Produtos ligado, Map<Long, Precos> precos, List<Produtos> semPreco) {
        Precos preco = precos.remove(antigo.id);
        if (preco != null) {
            preco.produtos = ligado;
            precos.put(ligado.id, preco);
        }
        // só chega aqui se o antigo tinha preço apenas deste mercado: depois de mover, ficou vazio
        semPreco.add(antigo);
    }

    // o MySQL compara texto sem diferenciar maiúsculas; o cache precisa fazer o mesmo para não duplicar produtos
    private static String chave(String nome, Long medidaId) {
        return nome.trim().toLowerCase(Locale.ROOT) + "|" + medidaId;
    }

    /**
     * Só dígitos e sem zeros à esquerda ("07891000462300" e "7891000462300" são o mesmo GTIN).
     * Devolve null para códigos ausentes ou curtos demais para serem um EAN/GTIN válido.
     */
    static String normalizarCodigoBarras(String codigo) {
        if (codigo == null) return null;
        String digitos = codigo.replaceAll("\\D", "").replaceFirst("^0+", "");
        return digitos.length() >= 8 ? digitos : null;
    }
}