import java.util.Arrays;
import java.util.LinkedList;

public class TrueAddy extends PokerrPlayer {

	final boolean DEBUG = false;
	final boolean vDEBUG = false;

	TrueAddy(PokerrMain parent) {
		super(parent);
	}

	LinkedList<Key> keys = new LinkedList<>();
	LinkedList<Key> activeKeys = new LinkedList<>();
	int startBank = 0;

	@Override
	int evaluate() {
		if (getPlayerHash(parent.committedPlayers()) == 0) {
			parent.qPrint(name + ": No players...");
			return 0;
		}
		
		int gameStage = getGameStage(parent.board);

		if (gameStage == 0) {
			if (!keys.isEmpty() && !keys.getLast().decided) {
				for (Key i : activeKeys) {
					i.returnAmt = bank - startBank;
					i.decided = true;
					parent.qPrint(i.toString());
				}
			}

			activeKeys.clear();
			startBank = bank;
		}

		parent.qPrint(name + ": Evaluating...");
		
		Key currentKey = new Key(
				this,
				holeCards, 
				parent.board, 
				getBet(), 
				getPlayerHash(parent.committedPlayers()),
				getGameStage(parent.board),
				0, //returnAmt
				1, //decision
				0  //bet
				);
		
		if (!keys.isEmpty() && currentKey.gameStage == keys.getLast().gameStage && !keys.getLast().decided) {
			parent.qPrint(name + ": Re-evaluating last key");
			keys.remove(keys.getLast());
		}
		
		parent.qPrint(name + ": Current Key:\n" + currentKey);

		double decision = 0;
		double totalWeight = 0;
		double avgBet = 0;
		int count = 0;
		for (Key i : keys) {
			if (activeKeys.contains(i)) continue;
			if (vDEBUG) System.out.println("" + i);
			
			double weight = 0;
			weight += Math.pow((100.0/12.0)*Math.abs(currentKey.holeCards[0].compareTo(i.holeCards[0])),2);
			weight += Math.pow((100.0/3.0)*Math.abs(currentKey.holeCards[0].compareToS(i.holeCards[0])),2);
			weight += Math.pow((100.0/12.0)*Math.abs(currentKey.holeCards[1].compareTo(i.holeCards[1])),2);
			weight += Math.pow((100.0/3.0)*Math.abs(currentKey.holeCards[1].compareToS(i.holeCards[1])),2);
			for (int j = 0; j < (gameStage == 0 ? 0 : 2 + gameStage); j++) {
				if (currentKey.board.length < 2 + gameStage || i.board.length < 2 + gameStage) continue;
				weight += Math.pow((100.0/12.0)*Math.abs(currentKey.board[j].compareTo(i.board[j])),2);
				weight += Math.pow((100.0/3.0)*Math.abs(currentKey.board[j].compareToS(i.board[j])),2);
			}
			weight += Math.pow((100.0/(parent.players.size()*STARTING_BANK))
					*Math.abs(currentKey.betFacing - i.betFacing),2);
			weight += Math.pow((100.0/parent.players.size())
					*Math.abs(playerHashDistance(currentKey.playerHash, i.playerHash)),2);
			if (vDEBUG) System.out.println("Weight: " + weight);
			
			weight += Math.pow((100.0/4.0)*(currentKey.gameStage - i.gameStage),2);
			if (vDEBUG) System.out.println("Weight: " + weight);
			
			if (weight == 0.0) count++;
			if (weight < 1 && weight >= 0) weight++;
			if (weight > -1 && weight < 0) weight--;
			
			weight = 1 / weight;
			if (vDEBUG) System.out.println("Weight: " + weight);
			
			weight *= (i.returnAmt >= 0 ? 1 : -1) + i.returnAmt/(parent.players.size()*STARTING_BANK);
			if (vDEBUG) System.out.println("Weight: " + weight);
			
			totalWeight += Math.abs(weight);
			decision += i.decision * weight;
			avgBet += i.bet * weight;
			
			if (vDEBUG) System.out.println("Decision: " + (decision / totalWeight));

			if (Double.isNaN(decision / totalWeight)) {
				throw new RuntimeException("decision = NaN");
			}
		}
		decision /= totalWeight;
		avgBet /= totalWeight;

		if (keys.isEmpty() || totalWeight == 0) decision = 1;
		
		if (count == 0) {
			parent.qPrint(name + ": Haven't seen this before.");
			decision += 0.5;
			avgBet = (avgBet + (Math.random())) / 2;
		}
		if (Double.isNaN(decision)) {
			throw new RuntimeException("decision == NaN");
		}
		parent.qPrint(name + ": I evaluate " + decision);

		int intDecision = decision >= 0 ? 0 : -1;
		currentKey.decision = intDecision == 0 ? 1 : -1;
		currentKey.bet = (int) avgBet;

		keys.add(currentKey);
		activeKeys.add(currentKey);

		int toReturn = intDecision;
		if (toReturn < 0) return -1;
		toReturn = (int) (currentKey.bet * bank);
		
		if (toReturn + getBet() < BB)
			toReturn = BB - getBet();
		if (toReturn + getBet() < getBet() * 2)
			toReturn = 0;
		if (toReturn + getBet() > bank) {
			toReturn = bank - (toReturn + getBet());
		}
		
		return toReturn;
	}

	int getPlayerHash(LinkedList<PokerrPlayer> x) {
		int hash = 0;
		for (PokerrPlayer i : x) hash += Math.pow(10, parent.players.indexOf(i));
		return hash;
	}

	int playerHashDistance(int x, int y) {
		int dif = 0;
		for (int i = parent.players.size() - 1; i >= 0; i--) {
			int pow = (int) Math.pow(10, i);
			if (!((x >= pow && y >= pow) || (x < pow && y < pow))) dif++;
			x %= pow;
			y %= pow;
		}
		return dif;
	}

	@Override
	void winFdbk(boolean win, Card[] winningHand, int potAmt) {
		if (!win 
				&& holeCards[0] != null
				&& keys.getLast().decision == -1 
				&& parent.decideWinner(bestHand(parent.board,true),winningHand) == -1) {
			for (Key i : activeKeys) {
				i.returnAmt = startBank / 2;
				i.decided = true;
			}
		} 
	}

}

class Key {
	Card[] holeCards;
	Card[] board;
	int betFacing;
	int playerHash;
	int gameStage;
	int returnAmt;
	int decision;
	boolean decided;
	double bet;

	PokerrPlayer parent;

	Key(PokerrPlayer parent, Card[] holeCards3, Card[] board3, int betFacing, int playerHash, int gameStage, int returnAmt, int decision, double bet) {
		parent.parent.qPrint(Arrays.toString(holeCards3));
		parent.parent.qPrint(Arrays.toString(board3));
		
		Card[] holeCards2 = Arrays.copyOf(holeCards3,holeCards3.length);
		Card[] board2 = Arrays.copyOf(board3, (gameStage == 0 ? 0 : 2 + gameStage));
		Arrays.sort(holeCards2);
		Arrays.sort(board2);
		
		parent.parent.qPrint(Arrays.toString(holeCards2));
		parent.parent.qPrint(Arrays.toString(board2));
		
		this.holeCards = holeCards2;
		this.board = board2;
		this.betFacing = betFacing;
		this.playerHash = playerHash;
		this.gameStage = gameStage;
		this.returnAmt = returnAmt;
		this.decision = decision;
		this.bet = bet;
		decided = false;

		this.parent = parent;
	}

	@Override
	public String toString() {
		StringBuilder toReturn = new StringBuilder("--Key--\n");
		toReturn.append("HoleCards: " + Arrays.toString(holeCards) + "\n");
		toReturn.append("Board: " + Arrays.toString(board) + "\n");
		toReturn.append("BetFacing: " + betFacing + "\n");
		toReturn.append("PlayerHash: " + playerHash + "\n");
		toReturn.append("GameStage: " + parent.parent.gameIndex[gameStage] + "\n");
		toReturn.append("ReturnAmt: " + returnAmt + "\n");
		toReturn.append("Decision: " + decision + "\n");
		toReturn.append("Decided: " + decided + "\n");
		toReturn.append("Bet: " + bet + "\n");
		return toReturn.toString();
	}

}
