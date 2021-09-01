import java.util.function.Supplier;

public class PokerLambda<T> extends PokerrPlayer
{
    private final Supplier<Integer> supplier;
    public PokerLambda(Supplier<Integer> supplier)
    {
        this.supplier = supplier;
    }

    @Override
    public int evaluate(int betfacing, Card[] board)
    {
        return this.supplier.get();
    }
}
