package br.com.conde.centralidade;

/**
 * O CONTADOR de visitas: quantos vertices e arestas foram olhados.
 *
 * <p>Visita, nunca segundo. Segundo muda com a maquina, com o que mais esta rodando e
 * com o humor do coletor de lixo, e a integracao continua roda em tres sistemas.
 * Visita e a mesma conta em qualquer lugar.
 *
 * <p>E e a visita que separa Brandes da definicao: a definicao de intermediacao
 * custa o cubo do numero de vertices, e Brandes custa vertices vezes arestas. Em
 * grafo esparso essa e a diferenca entre cabivel e nao cabivel, e ela so aparece
 * contando.
 */
public final class Contador {

    private long visitas;

    public void visitou() {
        visitas++;
    }

    public void visitou(long quantas) {
        visitas += quantas;
    }

    public long visitas() {
        return visitas;
    }

    public void zerar() {
        visitas = 0;
    }

    @Override
    public String toString() {
        return visitas + " visitas";
    }
}
