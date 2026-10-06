package org.scraper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
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

            Produtos produto = produtosRepository.findByNomeAndMedida(item.nome, medida.id);
            if (produto == null) {
                produto = new Produtos(item.nome, medida, categoria);
                produto.medida = item.quantidade;
                produtosRepository.persist(produto);
                criados++;
            } else if (produto.getCategoria() == null) {
                produto.setCategoria(categoria); // completa produtos antigos que estavam sem categoria
            }

            Precos precoExistente = precosRepository.findByMercadosAndProdutos(mercado, produto);
            if (precoExistente != null) {
                precoExistente.setPreco(item.preco);
                precoExistente.setUltimaAtualizacao(LocalDate.now());
                precoExistente.persist();
            } else {
                precosRepository.persist(new Precos(item.preco, mercado, produto));
            }
            atualizados++;
        }

        LOG.infof("[%s] sincronização concluída: %d produtos criados, %d preços atualizados, %d ignorados",
                nomeMercado, criados, atualizados, ignorados);
    }
}