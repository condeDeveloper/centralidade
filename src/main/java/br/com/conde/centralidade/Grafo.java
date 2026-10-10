package br.com.conde.centralidade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um GRAFO, dirigido ou nao, guardado como lista de adjacencia.
 *
 * <p>As medidas de centralidade respondem perguntas diferentes sobre o mesmo grafo, e
 * cada uma chama um vertice diferente de "o mais importante". Isso nao e defeito de
 * nenhuma delas: sao perguntas diferentes.
 *
 * <ul>
 *   <li>GRAU: quantos vizinhos. Quem conhece mais gente.
 *   <li>PROXIMIDADE: o inverso da soma das distancias. Quem chega rapido em todo
 *       mundo.
 *   <li>INTERMEDIACAO: em quantos caminhos minimos dos outros ele esta no meio. Quem
 *       e a ponte.
 *   <li>PAGERANK: para onde um passeio aleatorio volta mais. Quem e apontado por
 *       quem e apontado.
 * </ul>
 *
 * <p>A tabela do centro mostra as quatro no mesmo grafo, e em varios deles elas
 * discordam sobre quem esta no meio.
 */
public final class Grafo {

    private final String nome;
    private final int vertices;
    private final List<List<Integer>> saindo;
    private final List<List<Integer>> chegando;
    private final boolean dirigido;
    private final int arestas;

    private Grafo(String nome, int vertices, List<List<Integer>> saindo,
                  List<List<Integer>> chegando, boolean dirigido, int arestas) {

        this.nome = nome;
        this.vertices = vertices;
        this.saindo = saindo;
        this.chegando = chegando;
        this.dirigido = dirigido;
        this.arestas = arestas;
    }

    public static Construtor com(String nome, int vertices, boolean dirigido) {
        return new Construtor(nome, vertices, dirigido);
    }

    public static final class Construtor {

        private final String nome;
        private final int vertices;
        private final boolean dirigido;
        private final List<List<Integer>> saindo;
        private final List<List<Integer>> chegando;
        private int arestas;

        private Construtor(String nome, int vertices, boolean dirigido) {
            this.nome = nome;
            this.vertices = vertices;
            this.dirigido = dirigido;
            this.saindo = new ArrayList<>(vertices);
            this.chegando = new ArrayList<>(vertices);

            for (int i = 0; i < vertices; i++) {
                saindo.add(new ArrayList<>());
                chegando.add(new ArrayList<>());
            }
        }

        public Construtor aresta(int de, int para) {
            if (de < 0 || de >= vertices || para < 0 || para >= vertices)
                throw new IllegalArgumentException("vertice fora de 0 a " + (vertices - 1));

            saindo.get(de).add(para);
            chegando.get(para).add(de);
            arestas++;

            if (!dirigido && de != para) {
                saindo.get(para).add(de);
                chegando.get(de).add(para);
            }

            return this;
        }

        public Grafo pronto() {
            List<List<Integer>> s = new ArrayList<>(vertices);
            List<List<Integer>> c = new ArrayList<>(vertices);

            for (int i = 0; i < vertices; i++) {
                s.add(List.copyOf(saindo.get(i)));
                c.add(List.copyOf(chegando.get(i)));
            }

            return new Grafo(nome, vertices, List.copyOf(s), List.copyOf(c), dirigido, arestas);
        }
    }

    public String nome() {
        return nome;
    }

    public int vertices() {
        return vertices;
    }

    public int arestas() {
        return arestas;
    }

    public boolean dirigido() {
        return dirigido;
    }

    public List<Integer> saindoDe(int v) {
        return saindo.get(v);
    }

    public List<Integer> chegandoEm(int v) {
        return chegando.get(v);
    }

    public int grauDeSaida(int v) {
        return saindo.get(v).size();
    }

    /**
     * Os vertices SEM SAIDA, que sao o buraco por onde o PageRank vaza.
     *
     * <p>Num passeio aleatorio, chegar num vertice sem saida e ficar preso. A conta
     * ingenua do PageRank simplesmente perde a massa que entrou ali, e a soma das
     * notas deixa de ser um sem ninguem avisar.
     */
    public List<Integer> semSaida() {
        List<Integer> saida = new ArrayList<>();

        for (int v = 0; v < vertices; v++) if (saindo.get(v).isEmpty()) saida.add(v);

        return saida;
    }

    @Override
    public String toString() {
        return nome + " (" + vertices + " vertices, " + arestas + " arestas, "
                + (dirigido ? "dirigido" : "nao dirigido") + ")";
    }

    // ───────────────────────────── as formas ─────────────────────────────

    /** O CAMINHO: uma fila de vertices. O centro geometrico e o centro de tudo. */
    public static Grafo caminho(int n) {
        Construtor c = com("caminho de " + n, n, false);

        for (int i = 0; i + 1 < n; i++) c.aresta(i, i + 1);

        return c.pronto();
    }

    /**
     * A ESTRELA: um centro ligado a todos.
     *
     * <p>E o caso em que as quatro medidas concordam, e por isso ele nao separa nada.
     * Esta aqui como controle.
     */
    public static Grafo estrela(int n) {
        Construtor c = com("estrela de " + n, n, false);

        for (int i = 1; i < n; i++) c.aresta(0, i);

        return c.pronto();
    }

    /** O CICLO: todos iguais. Nenhum vertice e mais central que outro. */
    public static Grafo ciclo(int n) {
        Construtor c = com("ciclo de " + n, n, false);

        for (int i = 0; i < n; i++) c.aresta(i, (i + 1) % n);

        return c.pronto();
    }

    /**
     * O HALTERE: dois grupos fechados ligados por uma ponte.
     *
     * <p>E a forma que separa a INTERMEDIACAO das outras tres. Os vertices da ponte
     * tem grau baixo e proximidade media, e passam por eles TODOS os caminhos de um
     * lado para o outro.
     */
    public static Grafo haltere(int porLado) {
        int n = 2 * porLado + 1;
        Construtor c = com("haltere de " + porLado, n, false);

        for (int i = 0; i < porLado; i++)
            for (int j = i + 1; j < porLado; j++) c.aresta(i, j);

        for (int i = porLado + 1; i < n; i++)
            for (int j = i + 1; j < n; j++) c.aresta(i, j);

        c.aresta(porLado - 1, porLado);
        c.aresta(porLado, porLado + 1);

        return c.pronto();
    }

    /** O COMPLETO: todos ligados a todos. */
    public static Grafo completo(int n) {
        Construtor c = com("completo de " + n, n, false);

        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) c.aresta(i, j);

        return c.pronto();
    }

    /**
     * A TEIA DIRIGIDA COM BECO: um grafo dirigido com um vertice sem saida.
     *
     * <p>E a forma que mostra o vazamento do PageRank. O vertice do beco recebe e nao
     * devolve, e a conta ingenua perde a massa que chega la.
     */
    public static Grafo comBeco(int n) {
        Construtor c = com("com beco de " + n, n, true);

        for (int i = 0; i + 1 < n; i++) {
            c.aresta(i, i + 1);

            if (i > 0) c.aresta(i, 0);
        }

        return c.pronto();
    }

    /** Uma teia dirigida sem beco nenhum: todo vertice aponta para alguem. */
    public static Grafo dirigidoFechado(int n) {
        Construtor c = com("dirigido fechado de " + n, n, true);

        for (int i = 0; i < n; i++) {
            c.aresta(i, (i + 1) % n);
            c.aresta(i, (i + 3) % n);
        }

        return c.pronto();
    }

    /** Um grafo sorteado nao dirigido e conexo. */
    public static Grafo sorteado(int n, double chance, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        Construtor c = com("sorteado de " + n + " a " + chance + " (" + semente + ")", n, false);

        // uma arvore de base garante que o grafo e conexo
        for (int v = 1; v < n; v++) c.aresta(sorteio.ate(v), v);

        for (int u = 0; u < n; u++)
            for (int v = u + 1; v < n; v++)
                if (sorteio.fracao() < chance) c.aresta(u, v);

        return c.pronto();
    }

    public static Map<String, Grafo> todos() {
        Map<String, Grafo> mapa = new LinkedHashMap<>();

        for (Grafo g : List.of(
                caminho(5), caminho(9), estrela(7), ciclo(6), completo(5),
                haltere(4), haltere(6), sorteado(10, 0.2, 1), sorteado(12, 0.3, 2),
                comBeco(6), dirigidoFechado(6)))
            mapa.put(g.nome(), g);

        return mapa;
    }

    /**
     * O SORTEIO, escrito aqui em vez de vir da biblioteca.
     *
     * <p>A integracao continua roda em tres sistemas, e "o vertice mais central foi o
     * de numero 7" com grafos diferentes em cada sistema nao e uma medida. O
     * deslocamento de 32 bits tira os bits de baixo, que num congruente linear
     * alternam par e impar a cada passo e fariam as arestas sair num xadrez.
     */
    public static final class Sorteio {

        private long estado;

        public Sorteio(long semente) {
            this.estado = semente == 0 ? 1 : semente;
        }

        public int proximo() {
            estado = estado * 6364136223846793005L + 1442695040888963407L;
            return (int) (estado >>> 32);
        }

        public int ate(int limite) {
            return (int) (Integer.toUnsignedLong(proximo()) % limite);
        }

        public double fracao() {
            return Integer.toUnsignedLong(proximo()) / 4294967296.0;
        }
    }
}
