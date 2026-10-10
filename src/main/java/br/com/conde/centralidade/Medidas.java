package br.com.conde.centralidade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * As MEDIDAS, em visitas e em desvio, nunca em segundos.
 *
 * <p>Segundo nao e medida: muda com a maquina, com o que mais esta rodando e com o
 * humor do coletor de lixo. A integracao continua roda em tres sistemas, e tres
 * numeros de segundos nao se comparam.
 */
public final class Medidas {

    private Medidas() {
    }

    /** O amortecimento classico do PageRank, de 1998. */
    public static final double AMORTECIMENTO = 0.85;

    // ───────────────────── 1. contra a forca bruta ─────────────────────

    public record LinhaDefinicao(String grafo, int vertices, int arestas,
                                 double desvioDaIntermediacao, double desvioDoPageRank,
                                 boolean mesmoCentro, double somaDoPageRank) {
    }

    /**
     * Brandes contra a DEFINICAO, e a iteracao contra o SISTEMA LINEAR.
     *
     * <p>As duas colunas de desvio tem de ser praticamente zero. A da intermediacao e
     * entre duas contas exatas e teria de ser exatamente zero se nao fosse ponto
     * flutuante; a do PageRank e entre um metodo numerico e a solucao exata.
     */
    public static List<LinhaDefinicao> contraAForcaBruta() {
        List<LinhaDefinicao> linhas = new ArrayList<>();

        Grafo.todos().forEach((nome, g) -> {
            Contador contador = new Contador();

            double[] porBrandes = Centralidades.intermediacao(g, contador);
            double[] pelaDefinicao = Juiz.intermediacaoPelaDefinicao(g, contador);

            double[] porIteracao = Centralidades.pageRank(g, AMORTECIMENTO, 200, true);
            double[] peloSistema = Juiz.pageRankPeloSistema(g, AMORTECIMENTO, true);

            linhas.add(new LinhaDefinicao(nome, g.vertices(), g.arestas(),
                    Juiz.maiorDesvio(porBrandes, pelaDefinicao),
                    Juiz.maiorDesvio(porIteracao, peloSistema),
                    // compara o VALOR do maximo e nao o indice: num grafo simetrico
                    // todos os vertices empatam, e qual indice sai primeiro e detalhe
                    // de implementacao e nao desacordo
                    Math.abs(porBrandes[Centralidades.maior(porBrandes)]
                            - pelaDefinicao[Centralidades.maior(pelaDefinicao)]) < 1e-9,
                    Centralidades.soma(porIteracao)));
        });

        return linhas;
    }

    public record LinhaConhecida(String grafo, String medida, String esperado, String obtido, boolean bate) {
    }

    /**
     * As formas com RESPOSTA CONHECIDA, que dao para escrever sem rodar nada.
     *
     * <p>Na estrela, o centro tem intermediacao um e as pontas tem zero. No ciclo e no
     * completo, todos tem a mesma coisa. Sao conferencias que nao dependem de nenhuma
     * implementacao deste repositorio.
     */
    public static List<LinhaConhecida> asFormasConhecidas() {
        List<LinhaConhecida> linhas = new ArrayList<>();
        Contador contador = new Contador();

        Grafo estrela = Grafo.estrela(7);
        double[] naEstrela = Centralidades.intermediacao(estrela, contador);

        linhas.add(new LinhaConhecida("estrela de 7", "intermediacao do centro", "1.0",
                arredondar(naEstrela[0]), Math.abs(naEstrela[0] - 1) < 1e-9));

        boolean pontasZero = true;

        for (int v = 1; v < 7; v++) if (Math.abs(naEstrela[v]) > 1e-9) pontasZero = false;

        linhas.add(new LinhaConhecida("estrela de 7", "intermediacao das pontas", "0.0",
                arredondar(naEstrela[1]), pontasZero));

        for (Grafo g : List.of(Grafo.ciclo(6), Grafo.completo(5))) {
            double[] inter = Centralidades.intermediacao(g, contador);
            double[] prox = Centralidades.proximidade(g, contador);
            double[] rank = Centralidades.pageRank(g, AMORTECIMENTO, 200, true);

            linhas.add(new LinhaConhecida(g.nome(), "todos com a mesma intermediacao", "sim",
                    todosIguais(inter) ? "sim" : "NAO", todosIguais(inter)));
            linhas.add(new LinhaConhecida(g.nome(), "todos com a mesma proximidade", "sim",
                    todosIguais(prox) ? "sim" : "NAO", todosIguais(prox)));
            linhas.add(new LinhaConhecida(g.nome(), "todos com o mesmo PageRank", "sim",
                    todosIguais(rank) ? "sim" : "NAO", todosIguais(rank)));
        }

        Grafo completo = Grafo.completo(6);
        double[] noCompleto = Centralidades.proximidade(completo, contador);

        linhas.add(new LinhaConhecida("completo de 6", "proximidade de todos", "1.0",
                arredondar(noCompleto[0]), Math.abs(noCompleto[0] - 1) < 1e-9));

        return linhas;
    }

    public record LinhaTresContas(String grafo, int vertices, int arestas,
                                  int paresComMaisDeUmCaminho,
                                  double desvioDeBrandes, double desvioDaDefinicao) {
    }

    /**
     * AS TRES CONTAS DE INTERMEDIACAO contra a ENUMERACAO LITERAL.
     *
     * <p>A coluna que importa e a dos PARES COM MAIS DE UM CAMINHO MINIMO. Onde ela e
     * zero, qualquer jeito de contar caminhos da o mesmo numero, e a forma nao julga
     * nada: e o caso do caminho, da estrela e do haltere.
     *
     * <p>Foi por isso que o erro de contagem passou pelas formas desenhadas a mao e so
     * apareceu nos grafos sorteados. A tabela tem as duas metades de proposito.
     */
    public static List<LinhaTresContas> asTresContas() {
        List<LinhaTresContas> linhas = new ArrayList<>();
        Contador contador = new Contador();

        Map<String, Grafo> casos = new LinkedHashMap<>();

        for (Grafo g : List.of(Grafo.caminho(8), Grafo.estrela(7), Grafo.haltere(3),
                Grafo.ciclo(8), Grafo.completo(5), Grafo.dirigidoFechado(8)))
            casos.put(g.nome(), g);

        for (long semente : new long[] {1, 2, 3}) {
            Grafo g = Grafo.sorteado(8, 0.3, semente);

            casos.put(g.nome(), g);
        }

        casos.forEach((nome, g) -> {
            double[] porEnumeracao = Juiz.intermediacaoPorEnumeracao(g);

            linhas.add(new LinhaTresContas(nome, g.vertices(), g.arestas(),
                    Juiz.paresComMaisDeUmCaminhoMinimo(g),
                    Juiz.maiorDesvio(Centralidades.intermediacao(g, contador), porEnumeracao),
                    Juiz.maiorDesvio(Juiz.intermediacaoPelaDefinicao(g, contador), porEnumeracao)));
        });

        return linhas;
    }

    // ───────────────────── 2. o vazamento ─────────────────────

    public record LinhaVazamento(String grafo, int vertices, int semSaida,
                                 double somaComTratamento, double somaSemTratamento,
                                 double vazou, double desvioDoSistema) {
    }

    /**
     * O NO SEM SAIDA QUE VAZA.
     *
     * <p>Num passeio aleatorio, chegar num vertice sem saida e ficar preso. A conta
     * ingenua do PageRank perde a massa que entrou ali, e a soma das notas deixa de
     * ser um SEM NINGUEM AVISAR.
     *
     * <p>A coluna que importa e a terceira: quanto da probabilidade sumiu. Num grafo
     * sem beco ela e zero e as duas versoes sao a mesma coisa.
     */
    public static List<LinhaVazamento> oVazamento() {
        List<LinhaVazamento> linhas = new ArrayList<>();

        Map<String, Grafo> casos = new LinkedHashMap<>();

        for (int n : new int[] {4, 6, 10, 20}) casos.put("com beco de " + n, Grafo.comBeco(n));

        casos.put("dirigido fechado de 6", Grafo.dirigidoFechado(6));
        casos.put("caminho de 5", Grafo.caminho(5));
        casos.put("estrela de 7", Grafo.estrela(7));

        casos.forEach((nome, g) -> {
            double[] comTratamento = Centralidades.pageRank(g, AMORTECIMENTO, 200, true);
            double[] semTratamento = Centralidades.pageRank(g, AMORTECIMENTO, 200, false);
            double[] peloSistema = Juiz.pageRankPeloSistema(g, AMORTECIMENTO, true);

            double soma = Centralidades.soma(comTratamento);
            double somaSem = Centralidades.soma(semTratamento);

            linhas.add(new LinhaVazamento(nome, g.vertices(), g.semSaida().size(),
                    soma, somaSem, 1 - somaSem, Juiz.maiorDesvio(comTratamento, peloSistema)));
        });

        return linhas;
    }

    public record LinhaConvergencia(int rodadas, double desvioDoSistema, double soma) {
    }

    /**
     * Quantas rodadas a iteracao precisa para alcancar a solucao exata.
     *
     * <p>E a medida que so existe porque ha um juiz exato. Sem o sistema linear, o
     * criterio de parada seria "parou de mudar", que e outra coisa.
     */
    public static List<LinhaConvergencia> aConvergencia() {
        List<LinhaConvergencia> linhas = new ArrayList<>();
        Grafo g = Grafo.sorteado(30, 0.15, 9);
        double[] exato = Juiz.pageRankPeloSistema(g, AMORTECIMENTO, true);

        for (int rodadas : new int[] {1, 2, 5, 10, 20, 50, 100, 200}) {
            double[] iterado = Centralidades.pageRank(g, AMORTECIMENTO, rodadas, true);

            linhas.add(new LinhaConvergencia(rodadas, Juiz.maiorDesvio(iterado, exato),
                    Centralidades.soma(iterado)));
        }

        return linhas;
    }

    // ───────────────────── 3. o custo ─────────────────────

    public record LinhaCusto(int vertices, int arestas, long porBrandes, long pelaDefinicao,
                             double quantasVezesMenos) {
    }

    /**
     * BRANDES contra a DEFINICAO, em visitas.
     *
     * <p>A definicao custa o cubo do numero de vertices e nao olha para as arestas.
     * Brandes custa vertices vezes arestas. Em grafo esparso, a diferenca cresce com o
     * tamanho; em grafo denso, ela some.
     */
    public static List<LinhaCusto> oCusto() {
        List<LinhaCusto> linhas = new ArrayList<>();

        for (int n : new int[] {10, 20, 40, 60}) {
            Grafo g = Grafo.sorteado(n, 2.0 / n, 21);

            Contador deBrandes = new Contador();
            Contador daDefinicao = new Contador();

            Centralidades.intermediacao(g, deBrandes);
            Juiz.intermediacaoPelaDefinicao(g, daDefinicao);

            linhas.add(new LinhaCusto(n, g.arestas(), deBrandes.visitas(), daDefinicao.visitas(),
                    (double) daDefinicao.visitas() / deBrandes.visitas()));
        }

        return linhas;
    }

    // ───────────────────── 4. o centro ─────────────────────

    public record LinhaCentro(String grafo, int vertices, int porGrau, int porProximidade,
                              int porIntermediacao, int porPageRank, boolean asQuatroConcordam) {
    }

    /**
     * QUEM E O CENTRO de cada forma, pelas quatro medidas.
     *
     * <p>Elas discordam, e isso nao e defeito de nenhuma: sao perguntas diferentes. A
     * tabela mede em quantas formas elas concordam e em quantas nao.
     */
    public static List<LinhaCentro> oCentro() {
        List<LinhaCentro> linhas = new ArrayList<>();
        Contador contador = new Contador();

        Grafo.todos().forEach((nome, g) -> {
            int porGrau = Centralidades.maior(Centralidades.grau(g));
            int porProximidade = Centralidades.maior(Centralidades.proximidade(g, contador));
            int porIntermediacao = Centralidades.maior(Centralidades.intermediacao(g, contador));
            int porPageRank = Centralidades.maior(Centralidades.pageRank(g, AMORTECIMENTO, 200, true));

            boolean concordam = porGrau == porProximidade
                    && porGrau == porIntermediacao
                    && porGrau == porPageRank;

            linhas.add(new LinhaCentro(nome, g.vertices(), porGrau, porProximidade,
                    porIntermediacao, porPageRank, concordam));
        });

        return linhas;
    }

    public record LinhaHaltere(int porLado, int vertices, int ponte, double grauDaPonte,
                               double intermediacaoDaPonte, int postoPorGrau, int postoPorIntermediacao) {
    }

    /**
     * O HALTERE: o vertice da ponte tem o MENOR grau e a MAIOR intermediacao.
     *
     * <p>E a forma que separa as duas medidas de um jeito que nao da para discutir.
     * Quem conhece menos gente e exatamente quem nao pode faltar.
     */
    public static List<LinhaHaltere> oHaltere() {
        List<LinhaHaltere> linhas = new ArrayList<>();
        Contador contador = new Contador();

        for (int porLado : new int[] {3, 4, 6, 8}) {
            Grafo g = Grafo.haltere(porLado);
            int ponte = porLado;

            double[] grau = Centralidades.grau(g);
            double[] inter = Centralidades.intermediacao(g, contador);

            linhas.add(new LinhaHaltere(porLado, g.vertices(), ponte, grau[ponte], inter[ponte],
                    Juiz.ordenados(grau).indexOf(ponte) + 1,
                    Juiz.ordenados(inter).indexOf(ponte) + 1));
        }

        return linhas;
    }

    private static String arredondar(double valor) {
        return String.format(java.util.Locale.ROOT, "%.4f", valor);
    }

    private static boolean todosIguais(double[] valores) {
        for (int i = 1; i < valores.length; i++) if (Math.abs(valores[i] - valores[0]) > 1e-9) return false;

        return true;
    }
}
