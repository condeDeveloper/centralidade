package br.com.conde.centralidade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Os testes dos JUIZES.
 *
 * <p>Um juiz que ninguem confere nao e juiz: e so mais uma implementacao com a mesma
 * chance de estar errada. Este arquivo confere os quatro uns contra os outros e contra
 * respostas escritas a mao.
 */
class JuizTest {

    private final Contador contador = new Contador();

    // ───────────────── a enumeracao literal, o juiz do juiz ─────────────────

    /**
     * O TESTE QUE PEGOU O ERRO.
     *
     * <p>A primeira versao da {@link Juiz#intermediacaoPelaDefinicao} contava os
     * caminhos minimos DENTRO do laco de Floyd e Warshall, e isso conta duas vezes: um
     * caminho que passa por dois intermediarios e somado na iteracao de cada um deles.
     *
     * <p>Brandes e a definicao davam numeros diferentes nos grafos sorteados, e isso
     * so dizia que UMA das duas estava errada. A enumeracao literal, que nao tem
     * matriz nem recorrencia, disse qual.
     */
    @Test
    @DisplayName("as tres contas de intermediacao dao o mesmo numero em todo grafo de ate oito vertices")
    void asTresContasConcordam() {
        for (Grafo g : java.util.List.of(
                Grafo.caminho(5), Grafo.caminho(8), Grafo.estrela(7), Grafo.ciclo(6),
                Grafo.ciclo(7), Grafo.completo(5), Grafo.haltere(3), Grafo.comBeco(6),
                Grafo.dirigidoFechado(6), Grafo.dirigidoFechado(8))) {

            double[] porBrandes = Centralidades.intermediacao(g, contador);
            double[] pelaDefinicao = Juiz.intermediacaoPelaDefinicao(g, contador);
            double[] porEnumeracao = Juiz.intermediacaoPorEnumeracao(g);

            assertThat(Juiz.maiorDesvio(porBrandes, porEnumeracao))
                    .as("Brandes contra a enumeracao em %s", g.nome()).isLessThan(1e-9);

            assertThat(Juiz.maiorDesvio(pelaDefinicao, porEnumeracao))
                    .as("a definicao contra a enumeracao em %s", g.nome()).isLessThan(1e-9);
        }
    }

    /**
     * O CASO EXATO que a contagem dentro do laco de Floyd errava: um par com DOIS
     * caminhos minimos, e DOIS vertices no meio de cada um.
     *
     * <p>O desenho e um losango comprido, 0-1-2-5 e 0-3-4-5. E, olhado de perto, ele e
     * o ciclo de seis: foi essa percepcao que deu a resposta escrita a mao.
     *
     * <p>A conta a mao, por pares nao ordenados: seis pares a distancia um nao tem
     * ninguem no meio; seis pares a distancia dois tem um caminho minimo com um
     * intermediario cada, e dao seis de credito; tres pares opostos tem DOIS caminhos
     * minimos de dois intermediarios cada, e dao dois de credito cada um, seis no
     * total. Doze de credito repartidos por seis vertices iguais dao dois para cada,
     * e dois divididos pelo fator cinco vezes quatro sobre dois dao 0,2.
     *
     * <p>A versao errada contava quatro caminhos minimos no par oposto em vez de dois,
     * e saia com numero diferente desse.
     */
    @Test
    @DisplayName("no losango de dois intermediarios por caminho as tres contas dao 0,2 para todos")
    void oLosangoDeDoisIntermediariosPorCaminho() {
        Grafo g = Grafo.com("losango longo", 6, false)
                .aresta(0, 1).aresta(1, 2).aresta(2, 5)
                .aresta(0, 3).aresta(3, 4).aresta(4, 5)
                .pronto();

        double[] porEnumeracao = Juiz.intermediacaoPorEnumeracao(g);
        double[] pelaDefinicao = Juiz.intermediacaoPelaDefinicao(g, contador);
        double[] porBrandes = Centralidades.intermediacao(g, contador);

        for (int v = 0; v < 6; v++)
            assertThat(porEnumeracao[v]).as("vertice %d", v).isCloseTo(0.2, within(1e-12));

        assertThat(Juiz.maiorDesvio(porEnumeracao, pelaDefinicao)).isLessThan(1e-12);
        assertThat(Juiz.maiorDesvio(porEnumeracao, porBrandes)).isLessThan(1e-12);
    }

    /**
     * O mesmo desenho com um rabo pendurado, que tira a simetria.
     *
     * <p>Num grafo simetrico as tres contas podem estar erradas do MESMO jeito e
     * concordar. Aqui os seis valores sao diferentes entre si, e concordar vale mais.
     */
    @Test
    @DisplayName("com a simetria quebrada as tres contas continuam concordando")
    void oLosangoComRabo() {
        Grafo g = Grafo.com("losango com rabo", 8, false)
                .aresta(0, 1).aresta(1, 2).aresta(2, 5)
                .aresta(0, 3).aresta(3, 4).aresta(4, 5)
                .aresta(1, 6).aresta(6, 7)
                .pronto();

        double[] porEnumeracao = Juiz.intermediacaoPorEnumeracao(g);

        assertThat(Juiz.maiorDesvio(porEnumeracao, Juiz.intermediacaoPelaDefinicao(g, contador)))
                .isLessThan(1e-12);
        assertThat(Juiz.maiorDesvio(porEnumeracao, Centralidades.intermediacao(g, contador)))
                .isLessThan(1e-12);

        // e os valores sao mesmo diferentes entre si: o 1 e o 6 nao empatam com ninguem
        assertThat(porEnumeracao[1]).isGreaterThan(porEnumeracao[6]).isGreaterThan(porEnumeracao[0]);
        assertThat(porEnumeracao[7]).isZero();
    }

    /**
     * A VARREDURA SORTEADA, que e o teste que mais vale deste arquivo.
     *
     * <p>Nenhuma das formas desenhadas a mao pegava o erro de contagem: no caminho, na
     * estrela e no ciclo ou ha um caminho minimo so entre cada par ou ha simetria total,
     * e os dois jeitos de contar dao o mesmo numero. Foi um grafo sorteado que pegou.
     *
     * <p>Por isso a varredura esta aqui e nao so as formas bonitas. Oitenta grafos
     * sorteados de sete e oito vertices, em quatro densidades, contra a enumeracao
     * literal.
     */
    @Test
    @DisplayName("em oitenta grafos sorteados as tres contas de intermediacao concordam")
    void aVarreduraSorteadaConcorda() {
        int conferidos = 0;

        for (int n : new int[] {7, 8})
            for (double chance : new double[] {0.1, 0.2, 0.35, 0.6})
                for (long semente = 1; semente <= 10; semente++) {
                    Grafo g = Grafo.sorteado(n, chance, semente);

                    double[] porEnumeracao = Juiz.intermediacaoPorEnumeracao(g);

                    assertThat(Juiz.maiorDesvio(Centralidades.intermediacao(g, contador), porEnumeracao))
                            .as("Brandes em %s", g.nome()).isLessThan(1e-9);

                    assertThat(Juiz.maiorDesvio(Juiz.intermediacaoPelaDefinicao(g, contador), porEnumeracao))
                            .as("a definicao em %s", g.nome()).isLessThan(1e-9);

                    conferidos++;
                }

        assertThat(conferidos).isEqualTo(80);
    }

    /**
     * E a mesma varredura para o PAGERANK, contra a solucao exata do sistema linear.
     *
     * <p>Aqui da para ir mais longe, porque a eliminacao aguenta trezentos vertices.
     */
    @Test
    @DisplayName("em sessenta grafos sorteados a iteracao alcanca a solucao exata do sistema")
    void aVarreduraDoPageRank() {
        for (int n : new int[] {10, 25, 60})
            for (double chance : new double[] {0.05, 0.15, 0.4})
                for (long semente = 1; semente <= 7; semente++) {
                    Grafo g = Grafo.sorteado(n, chance, semente);

                    double[] porIteracao = Centralidades.pageRank(g, Medidas.AMORTECIMENTO, 200, true);
                    double[] peloSistema = Juiz.pageRankPeloSistema(g, Medidas.AMORTECIMENTO, true);

                    assertThat(Juiz.maiorDesvio(porIteracao, peloSistema)).as(g.nome()).isLessThan(1e-12);
                    assertThat(Centralidades.soma(peloSistema)).as("soma em %s", g.nome())
                            .isCloseTo(1.0, within(1e-9));
                }
    }

    @Test
    @DisplayName("a enumeracao recusa grafo com mais de oito vertices")
    void aEnumeracaoTemLimite() {
        assertThatThrownBy(() -> Juiz.intermediacaoPorEnumeracao(Grafo.caminho(9)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 vertices");
    }

    @Test
    @DisplayName("a definicao recusa grafo com mais de sessenta vertices")
    void aDefinicaoTemLimite() {
        assertThatThrownBy(() -> Juiz.intermediacaoPelaDefinicao(Grafo.caminho(61), contador))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("60 vertices");
    }

    // ───────────────── as distancias de todos para todos ─────────────────

    @Test
    @DisplayName("Floyd e Warshall da as mesmas distancias que a busca em largura")
    void floydBateComABuscaEmLargura() {
        Grafo.todos().forEach((nome, g) -> {
            long[][] porFloyd = Juiz.distanciasDeTodosParaTodos(g);

            for (int origem = 0; origem < g.vertices(); origem++) {
                int[] porBusca = Centralidades.distanciasDe(g, origem, contador);

                for (int v = 0; v < g.vertices(); v++) {
                    boolean alcancavel = porBusca[v] >= 0;

                    assertThat(porFloyd[origem][v] < Long.MAX_VALUE / 4)
                            .as("%s: %d alcanca %d?", nome, origem, v).isEqualTo(alcancavel);

                    if (alcancavel)
                        assertThat(porFloyd[origem][v]).as("%s: de %d ate %d", nome, origem, v)
                                .isEqualTo(porBusca[v]);
                }
            }
        });
    }

    // ───────────────── o sistema linear do PageRank ─────────────────

    @Test
    @DisplayName("a eliminacao resolve um sistema de resposta conhecida")
    void aEliminacaoResolve() {
        // 2x + y = 5 ; x - y = 1  →  x = 2, y = 1
        double[] x = Juiz.resolver(new double[][] {{2, 1, 5}, {1, -1, 1}});

        assertThat(x[0]).isCloseTo(2.0, within(1e-12));
        assertThat(x[1]).isCloseTo(1.0, within(1e-12));
    }

    /**
     * O PIVOTEAMENTO PARCIAL nao e enfeite: sem a troca de linhas, um zero na diagonal
     * divide por zero e um pivo quase zero faz o arredondamento explodir.
     */
    @Test
    @DisplayName("a eliminacao resolve mesmo com zero na diagonal")
    void oPivoteamentoTrocaLinhas() {
        // a primeira equacao nao tem x: sem trocar linhas, divide por zero
        double[] x = Juiz.resolver(new double[][] {{0, 2, 4}, {3, 1, 7}});

        assertThat(x[0]).isCloseTo(5.0 / 3, within(1e-12));
        assertThat(x[1]).isCloseTo(2.0, within(1e-12));
    }

    @Test
    @DisplayName("um sistema sem solucao unica reclama em vez de devolver numero inventado")
    void oSistemaSingularReclama() {
        assertThatThrownBy(() -> Juiz.resolver(new double[][] {{1, 2, 3}, {2, 4, 6}}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("solucao unica");
    }

    /**
     * O JUIZ MAIS FORTE do repositorio: a iteracao de potencia se APROXIMA da resposta
     * e a eliminacao E a resposta.
     */
    @Test
    @DisplayName("a iteracao alcanca a solucao exata do sistema em todo grafo")
    void aIteracaoAlcancaOSistema() {
        Grafo.todos().forEach((nome, g) -> {
            double[] porIteracao = Centralidades.pageRank(g, Medidas.AMORTECIMENTO, 200, true);
            double[] peloSistema = Juiz.pageRankPeloSistema(g, Medidas.AMORTECIMENTO, true);

            assertThat(Juiz.maiorDesvio(porIteracao, peloSistema)).as(nome).isLessThan(1e-12);
            assertThat(Centralidades.soma(peloSistema)).as("soma em %s", nome).isCloseTo(1.0, within(1e-9));
        });
    }

    @Test
    @DisplayName("o sistema tambem resolve a versao sem tratamento, que vaza")
    void oSistemaSemTratamento() {
        Grafo g = Grafo.comBeco(6);

        double[] porIteracao = Centralidades.pageRank(g, Medidas.AMORTECIMENTO, 200, false);
        double[] peloSistema = Juiz.pageRankPeloSistema(g, Medidas.AMORTECIMENTO, false);

        assertThat(Juiz.maiorDesvio(porIteracao, peloSistema)).isLessThan(1e-12);
        assertThat(Centralidades.soma(peloSistema)).isLessThan(0.9);
    }

    @Test
    @DisplayName("o sistema devolve vazio para grafo sem vertice e recusa grafo grande demais")
    void osLimitesDoSistema() {
        assertThat(Juiz.pageRankPeloSistema(Grafo.com("vazio", 0, false).pronto(), 0.85, true)).isEmpty();

        assertThatThrownBy(() -> Juiz.pageRankPeloSistema(Grafo.caminho(301), 0.85, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300 vertices");
    }

    // ───────────────── a comparacao ─────────────────

    @Test
    @DisplayName("o maior desvio e infinito quando os vetores tem tamanhos diferentes")
    void oMaiorDesvioDeTamanhosDiferentes() {
        assertThat(Juiz.maiorDesvio(new double[] {1, 2}, new double[] {1, 2, 3})).isInfinite();
        assertThat(Juiz.maiorDesvio(new double[] {1, 5}, new double[] {1, 2})).isEqualTo(3.0);
    }

    @Test
    @DisplayName("os ordenados saem do maior para o menor, desempatando pelo indice")
    void osOrdenados() {
        assertThat(Juiz.ordenados(new double[] {1, 9, 9, 3})).containsExactly(1, 2, 3, 0);
    }

    @Test
    @DisplayName("a mesma ordem aceita troca entre valores empatados e recusa troca de verdade")
    void aMesmaOrdem() {
        assertThat(Juiz.mesmaOrdem(new double[] {5, 5, 1}, new double[] {5, 5, 1}, 1e-9)).isTrue();
        assertThat(Juiz.mesmaOrdem(new double[] {5, 1}, new double[] {1, 5}, 1e-9)).isFalse();
        assertThat(Juiz.mesmaOrdem(new double[] {5, 1}, new double[] {1, 5, 3}, 1e-9)).isFalse();
    }
}
