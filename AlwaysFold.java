
public class AlwaysFold extends PokerrPlayer{
	
	/*
	 * Always folds.
	 */
	
	@Override
	public int evaluate(int betfacing, Card[] board) {
		return -1;
	}
	
}
