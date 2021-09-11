import java.util.Arrays;

public abstract class PokerrPlayer {

	final int BB = 500;
	final int SB = BB / 2;
	final int STARTING_BANK = 10000;
	public int bank = 0;
	public Card[] holeCards = new Card[2];
	public boolean inTheHand = true;
	public boolean allIn = false;
	public double totalWinnings = 0;
	public String name = "";
	public int frontMoney = 0;

	PokerrPlayer() {
		bank = STARTING_BANK;
	}

	public double getTotalWinnings() {
		return totalWinnings;
	}

	// return -1 = fold
	// return 0 = call
	// return >0 = bet/raise
	abstract int evaluate(int betfacing, Card[] board);

	void winFdbk(boolean win, int[] str) {
		
	}

	public int[] strength(Card[] ce) {
		if (ce.length == 2 || ce[2] == null) {
			int[] CEStrength = new int[] {0,0,0,0};
			if (ce[0].value == ce[1].value)
				CEStrength[0] = 1;
			CEStrength[2] = ce[0].value >= ce[1].value ? ce[0].value : ce[1].value;
			return CEStrength;
		}
		
		
		int[] CEStrength = new int[] {0,0,0,0};
		CEStrength[2] = ce[0].value;
		//pair
		if (ce[0].value == ce[1].value) {
			CEStrength[0] = 1;
			CEStrength[2] = ce[0].value;
			//two pair
			if (ce[3] != null && ce[2].value == ce[3].value) {
				CEStrength[0] = 2;
				CEStrength[2] = ce[0].value >= ce[2].value ? ce[0].value : ce[2].value;
				CEStrength[3] = ce[0].value <= ce[2].value ? ce[0].value : ce[2].value;
			}

			//trips
			if (ce[1].value == ce[2].value) {
				CEStrength[0] = 3;
				CEStrength[2] = ce[0].value;

				//full house
				if (ce[3] != null && ce[4] != null && ce[3].value == ce[4].value) {
					CEStrength[0] = 6;
					CEStrength[3] = ce[3].value;
				}

				//quads
				if (ce[3] != null && ce[2].value == ce[3].value) {
					CEStrength[0] = 7;
					CEStrength[2] = ce[0].value;
				}
			}
		} // if

		//straight
		if (ce[3] != null && ce[4] != null && ce[0].value == ce[1].value + 1
				&&	ce[1].value == ce[2].value + 1
				&&	ce[2].value == ce[3].value + 1
				&&	ce[3].value == ce[4].value + 1) {
			if (CEStrength[0] < 4) {
				CEStrength[0] = 4;
				CEStrength[2] = ce[0].value;
			}
			//straight flush
			if (ce[3] != null && ce[4] != null && ce[0].suit == ce[1].suit 
					&& ce[1].suit == ce[2].suit 
					&& ce[2].suit == ce[3].suit 
					&& ce[3].suit == ce[4].suit) {
				CEStrength[0] = 8;
				CEStrength[2] = ce[0].value;
				if (ce[0].value == 14) {
					CEStrength[0] = 9;
				}
			}
			//wheel straight
		} else if (ce[3] != null && ce[4] != null && ce[0].value == 5
				&&	ce[1].value == 4
				&&	ce[2].value == 3
				&&	ce[3].value == 2
				&&	ce[4].value == 14) {
			if (CEStrength[0] < 4) {
				CEStrength[0] = 4;
				CEStrength[2] = ce[0].value;
			}
			//straight wheel flush
			if (ce[3] != null && ce[4] != null && ce[0].suit == ce[1].suit 
					&& ce[1].suit == ce[2].suit 
					&& ce[2].suit == ce[3].suit 
					&& ce[3].suit == ce[4].suit) {
				CEStrength[0] = 8;
				CEStrength[2] = ce[0].value;
				if (ce[0].value == 14) {
					CEStrength[0] = 9;
				}
			}
		}

		//flush
		if (ce[3] != null && ce[4] != null && ce[0].suit == ce[1].suit 
				&& ce[1].suit == ce[2].suit 
				&& ce[2].suit == ce[3].suit 
				&& ce[3].suit == ce[4].suit) {
			if (CEStrength[0] < 5) {
				CEStrength[0] = 5;
				CEStrength[2] = ce[0].value;
			}
		}

		return CEStrength;
	} // strength




	public Card[] bestHand(Card[] board, boolean useHoleCards) {
		Card[] toReturn = new Card[5];
		Card[] ce = new Card[5];
		int[] CEStrength = new int[]{-1,-1};
		int[] TRStrength = new int[]{-1,-1};
		if (board[0] == null) {
			return Arrays.copyOf(holeCards, 2);
		} else {
			//hand rankings			
			int count = useHoleCards ? 2 : 0;
			for (Card c : board) {
				if (c != null)
					count++;
			}
			Card[] raw = new Card[count];
			if (useHoleCards) {
				raw[0] = holeCards[0];
				raw[1] = holeCards[1];
			}
			for (int i = 0; i < 5; i++) {
				if (board[i] != null)
					raw[i + (useHoleCards ? 2 : 0)] = board[i];
			}
			Permutations<Card> perm = new Permutations<Card>(raw);
			//Card[] last = null;
			while(perm.hasNext()) {
				Card[] ce2 = perm.next();
				ce = Arrays.copyOf(ce2, 5);

				boolean equSuit = true;
				for (int i = 0; i < 5; i++) {
					if (ce[i] != null && toReturn[i] != null) {
						if (toReturn[i].suit != ce[i].suit) {	//|| (last != null && last[i].suit != ce[i].suit)) {
							equSuit = false;
							break;
						}
					}
				}
				if (equSuit && Arrays.equals(ce, toReturn))// && last != null && Arrays.equals(ce, last))
					continue;
				
				CEStrength = strength(ce);
				
				if (CEStrength[0] < TRStrength[0])
					continue;
				
				//last = ce;
				if (CEStrength[0] > TRStrength[0]) {
					TRStrength = CEStrength;
					toReturn = ce;
				} else if (CEStrength[0] == TRStrength[0] && CEStrength[2] > TRStrength[2]) {
					TRStrength = CEStrength;
					toReturn = ce;
				} else if (CEStrength[0] == TRStrength[0] && CEStrength[2] == TRStrength[2] && CEStrength[3] > TRStrength[3]) {
					TRStrength = CEStrength;
					toReturn = ce;
				} else if (CEStrength[0] == TRStrength[0] && CEStrength[2] == TRStrength[2] && CEStrength[3] == TRStrength[3]) {
					if (CEStrength[0] != 6) {
						if (ce[4] != null && CEStrength[0] == 2 && ce[4].value > toReturn[4].value) {
							toReturn = ce;
						} else if (CEStrength[0] == 0
								|| CEStrength[0] == 1
								|| CEStrength[0] == 2
								|| CEStrength[0] == 3
								|| CEStrength[0] == 7) {
							if (Arrays.compare(toReturn, ce) > 0) {
								toReturn = ce;
							}
						}
					}
				} // if
			}
		}
		return toReturn;
	} // bestHand

	public int getGameStage(Card[] board) {
		if (board[0] == null)
			return 0;
		if (board[3] == null)
			return 1;
		if (board[4] == null)
			return 2;
		return 3;

	}
	

}
