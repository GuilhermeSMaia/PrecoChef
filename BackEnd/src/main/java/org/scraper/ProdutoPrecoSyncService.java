package org.scraper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.entity.Categoria;
import org.entity.Medidas;
import org.entity.Mercados;
import org.entity.Precos;
import org.entity.Produtos;
import org.jboss.logging.Logger;
import org.repository.CategoriaRepository;
import org.repository.MedidasRepository;
import org.repository.MercadoRepository;
import org.repository.PrecosRepository;
import org.repository.ProdutosRepository;
import org.scraper.dto.ScrapedProdutoDTO;

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

        int criados = 0, atualizados = 0, ignorados = 0;

        // cache local para não consultar o banco a cada item da mesma categoria/medida
        Map<String, Categoria> categorias = new HashMap<>();
        Map<String, Medidas> medidas = new HashMap<>();

        // produtos e preços carregados de uma vez só: consultar o banco item a item dentro da
        // transação força um flush a cada consulta e deixa a sincronização quadrática (muito lenta)
        Map<String, Produtos> produtos = new HashMap<>();
        for (Produtos p : produtosRepository.listAllComMedida()) {
            produtos.putIfAbsent(chave(p.nome, p.getMedidas().id), p);
        }
        Map<Long, Precos> precos = new HashMap<>();
        for (Precos p : precosRepository.listByMercado(mercado)) {
            precos.putIfAbsent(p.getProdutos().id, p);
        }

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

            Produtos produto = produtos.get(chave(item.nome, medida.id));
            if (produto == null) {
                produto = new Produtos(item.nome, medida, categoria);
                produto.medida = item.quantidade;
                produto.setMarca(item.marca);
                produtosRepository.persist(produto);
                produtos.put(chave(item.nome, medida.id), produto);
                criados++;
            } else if (produto.getCategoria() == null) {
                produto.setCategoria(categoria); // completa produtos antigos que estavam sem categoria
            }

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

        LOG.infof("[%s] sincronização concluída: %d produtos criados, %d preços atualizados, %d ignorados",
                nomeMercado, criados, atualizados, ignorados);
    }

    // o MySQL compara texto sem diferenciar maiúsculas; o cache precisa fazer o mesmo para não duplicar produtos
    private static String chave(String nome, Long medidaId) {
        return nome.trim().toLowerCase(Locale.ROOT) + "|" + medidaId;
    }
}