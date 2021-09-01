
public class AlwaysMinBet extends PokerrPlayer {
	
	/*
	 * Always minbets.
	 * Never folds.
	 */
	
	@Override
	public int evaluate(int betfacing, Card[] board) {
		int evaluation;
		if (betfacing < (BB/2)+1) {
			evaluation = BB;
		} else {
			evaluation = 0;
		}
		if (evaluation > bank)
			return bank;
		return evaluation;
	}

}
