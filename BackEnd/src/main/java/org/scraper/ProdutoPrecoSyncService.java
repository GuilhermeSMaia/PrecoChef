package org.scraper;

import java.time.LocalDate;
import java.util.List;

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

        for (ScrapedProdutoDTO item : itens) {
            Categoria categoria = categoriaRepository.findByCategoria(item.categoria);
            Medidas medida = medidasRepository.findByMedida(item.medida);

            if (categoria == null || medida == null) {
                ignorados++;
                continue; // categoria/medida precisam existir previamente (cadastro controlado)
            }

            Produtos produto = produtosRepository.findByNomeAndMedida(item.nome, medida.id);
            if (produto == null) {
                produto = new Produtos(item.nome, medida, categoria);
                produtosRepository.persist(produto);
                criados++;
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