import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class PokerPlayer {

	final int BB = 500;
	final int SB = BB / 2;
	final int STARTING_BANK = 10000;
	public int bank;
	public Card[] holeCards = new Card[2];
	public boolean inTheHand = true;
	public boolean allIn = false;
	public double totalWinnings = 0;
	public String name = "";
	public int frontMoney = 0;
	public PokerMain parent;
	public LinkedList<Double> winningHistory;
	public Card[] bh;
	
	PokerPlayer(PokerMain parent) {
		this.parent = parent;
		bank = STARTING_BANK;
		winningHistory = new LinkedList<>();
	}

	public double winningsSlope() {
		double sum = 0;
		int lookback = 150	;
		if (winningHistory.size() > lookback) {
			for (int i = winningHistory.size() - 1; i > winningHistory.size() - (lookback + 1); i--) {
				sum += winningHistory.get(i) - winningHistory.get(i - 1);
			}
			sum /= lookback;
		}
		return sum;
	}

	public double getTotalWinnings() {
		return totalWinnings;
	}

	// return -1 = fold
	// return 0 = call
	// return >0 = bet/raise
	abstract int evaluate();

	void winFdbk(boolean win, Card[] winningHand, int potAmt) {
		// do nothing by default
	}

	public static int[] strength(Card[] ce) {
		if (ce.length == 2 || ce[2] == null) {
			int[] CEStrength = new int[] {0,0,0,0};
			if (ce[0].value == ce[1].value)
				CEStrength[0] = 1;
			CEStrength[2] = Math.max(ce[0].value, ce[1].value);
			return CEStrength;
		}


		int[] CEStrength = new int[] {0,0,0,0};
		CEStrength[2] = ce[0].value;

		//optimization: anything satisfying this condition can not make a hand better than high card
		if (ce[0].suit != ce[1].suit && !(ce[0].value == ce[1].value || ce[0].value == ce[1].value + 1)) return CEStrength;

		//pair
		if (ce[0].value == ce[1].value) {
			CEStrength[0] = 1;
			//two pair
			if (ce.length >= 4 && ce[3] != null && ce[2].value == ce[3].value) {
				CEStrength[0] = 2;
				CEStrength[2] = Math.max(ce[0].value, ce[2].value);
				CEStrength[3] = Math.min(ce[0].value, ce[2].value);
			}

			//trips
			if (ce[1].value == ce[2].value) {
				CEStrength[0] = 3;

				//full house
				if (ce.length >= 5 && ce[4] != null && ce[3].value == ce[4].value) {
					CEStrength[0] = 6;
					CEStrength[3] = ce[3].value;
					return CEStrength;
				}

				//quads
				if (ce.length >= 4 && ce[3] != null && ce[2].value == ce[3].value) {
					CEStrength[0] = 7;
					return CEStrength;
				}
			}
		} // if

		if (ce.length >= 5 && ce[4] != null) {
			//straight
			if (ce[0].value == ce[1].value + 1
					&& ce[1].value == ce[2].value + 1
					&& ce[2].value == ce[3].value + 1
					&& ce[3].value == ce[4].value + 1) {
				CEStrength[0] = 4;
				//straight flush
				if (ce[0].suit == ce[1].suit && ce[1].suit == ce[2].suit && ce[2].suit == ce[3].suit && ce[3].suit == ce[4].suit) {
					CEStrength[0] = 8;
					if (ce[0].value == 14) {
						CEStrength[0] = 9;
					}
					return CEStrength;
				}

				//wheel straight
			} else if (ce[0].value == 5
					&& ce[1].value == 4
					&& ce[2].value == 3
					&& ce[3].value == 2
					&& ce[4].value == 14) {
				CEStrength[0] = 4;
				//straight wheel flush
				if (ce[0].suit == ce[1].suit
						&& ce[1].suit == ce[2].suit
						&& ce[2].suit == ce[3].suit
						&& ce[3].suit == ce[4].suit) {
					CEStrength[0] = 8;
					return CEStrength;
				}
			}

			//flush
			if (ce[0].suit == ce[1].suit
					&& ce[1].suit == ce[2].suit
					&& ce[2].suit == ce[3].suit
					&& ce[3].suit == ce[4].suit) {
				if (CEStrength[0] < 5) {
					CEStrength[0] = 5;
					CEStrength[2] = ce[0].value;
				}
			}
		}

		return CEStrength;
	} // strength

	public Card[] bestHand(Card[] board, boolean useHoleCards) {
		Card[] toReturn = new Card[7];
		Card[] ce = new Card[0];
		int[] CEStrength;
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
			for (int i = 0; i < count - (useHoleCards ? 2 : 0); i++) {
				if (board[i] != null)
					raw[i + (useHoleCards ? 2 : 0)] = board[i];
			}
			Permutations<Card> perm = new Permutations<>(raw);
			while (perm.hasNext()) {
				ce = perm.next();
				if (toReturn[0] == null) {
					toReturn = Arrays.copyOf(ce, ce.length);
				}
				boolean equ = true;
				for (int i = 0; i < Math.min(ce.length,5); i++) {
					if (toReturn[i].suit != ce[i].suit || toReturn[i].value != ce[i].value) {
						equ = false;
						break;
					}
				}
				if (equ) continue;

				CEStrength = strength(ce);

				if (CEStrength[0] < TRStrength[0])
					continue;

				if (CEStrength[0] > TRStrength[0]) {
					TRStrength = CEStrength;
					toReturn = Arrays.copyOf(ce,ce.length);
				} else if (CEStrength[2] > TRStrength[2]) {
					TRStrength = CEStrength;
					toReturn = Arrays.copyOf(ce,ce.length);
				} else if (CEStrength[2] == TRStrength[2] && CEStrength[3] > TRStrength[3]) {
					TRStrength = CEStrength;
					toReturn = Arrays.copyOf(ce,ce.length);
				} else if (CEStrength[2] == TRStrength[2] && CEStrength[3] == TRStrength[3]) {
					if (CEStrength[0] != 6) {
						if (ce.length >= 5 && CEStrength[0] == 2 && ce[4].value > toReturn[4].value) {
							toReturn = Arrays.copyOf(ce,ce.length);
						} else if (CEStrength[0] == 0
								|| CEStrength[0] == 1
								|| CEStrength[0] == 2
								|| CEStrength[0] == 3
								|| CEStrength[0] == 7) {
							if (Arrays.compare(toReturn, ce) > 0) {
								toReturn = Arrays.copyOf(ce,ce.length);
							}
						}
					}
				} // if
			}
		}
		return Arrays.copyOf(toReturn,5);
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

	public int getBet() {
		int bet =0;
		for (Pot i : parent.pots) {
			bet += i.bet;
		}
		return bet;
	}

	public int getPot() {
		int pot =0;
		for (Pot i : parent.pots) {
			pot += i.potAmt;
		}
		return pot;
	}

	int sanitizeBetV(int tr) {
		int toReturn = tr;
		if (getBet() * 1.5 > toReturn) return -1;
		if (getBet()> toReturn) return 0;
		if (getBet() == toReturn) return 0;
		toReturn = toReturn - getBet();
		if (toReturn + getBet() < BB)
			toReturn = BB - getBet();
		if (toReturn + getBet() < getBet() * 2)
			toReturn = 0;
		if (toReturn + getBet() > bank)
			toReturn = bank - (toReturn + getBet());
		if (toReturn < 0)
			toReturn = 0;
		return toReturn;
	}

	int sanitizeBet(int tr) {
		int toReturn = tr;
		if (toReturn + getBet() < BB)
			toReturn = BB - getBet();
		if (toReturn + getBet() < getBet() * 2)
			toReturn = 0;
		if (toReturn + getBet() > bank)
			toReturn = bank - (toReturn + getBet());
		if (toReturn < 0)
			toReturn = 0;
		return toReturn;
	}

}
