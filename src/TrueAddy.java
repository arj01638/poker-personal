import java.util.Arrays;
import java.util.LinkedList;

public class TrueAddy extends PokerrPlayer {

	final boolean DEBUG = true;

	TrueAddy(PokerrMain parent) {
		super(parent);
	}

	LinkedList<Key> keys = new LinkedList<>();
	LinkedList<Key> activeKeys = new LinkedList<>();
	int startBank = 0;

	@Override
	int evaluate() {
		int gameStage = getGameStage(parent.board);

		if (gameStage == 0) {
			if (!keys.getLast().decided) {
				for (Key i : activeKeys) {
					i.returnAmt = Math.abs(bank - startBank);
					parent.qPrint(i.toString());
					i.decided = true;
				}
			}

			activeKeys.clear();
			startBank = bank;
		}

		Key currentKey = new Key(
				holeCards, 
				parent.board, 
				getBet(), 
				getPlayerHash(parent.committedPlayers()),
				getGameStage(parent.board),
				0,//returnAmt
				1 //decision
				);
		parent.qPrint(name + ": Current Key,\n" + currentKey);

		double decision = 0;
		double totalWeight = 0;
		for (Key i : keys) {
			double weight = 0;
			weight += (100.0/12.0)*Math.abs(currentKey.holeCards[0].compareTo(i.holeCards[0]));
			weight += (100.0/3.0)*Math.abs(currentKey.holeCards[0].compareToS(i.holeCards[0]));
			weight += (100.0/12.0)*Math.abs(currentKey.holeCards[1].compareTo(i.holeCards[1]));
			weight += (100.0/3.0)*Math.abs(currentKey.holeCards[1].compareToS(i.holeCards[1]));
			for (int j = 0; j < 5; j++) {
				weight += (100.0/12.0)*Math.abs(currentKey.board[j].compareTo(i.board[j]));
				weight += (100.0/3.0)*Math.abs(currentKey.board[j].compareToS(i.board[j]));
			}
			weight += (100.0/parent.players.size()*STARTING_BANK)
					*Math.abs(currentKey.betFacing - i.betFacing);
			weight += (100.0/parent.players.size())
					*Math.abs(playerHashDistance(currentKey.playerHash, i.playerHash));
			if (DEBUG) parent.qPrint(currentKey.playerHash + "," + i.playerHash + ","
					+ Math.abs(playerHashDistance(currentKey.playerHash, i.playerHash)));
			weight += (100.0/4.0)*currentKey.gameStage - i.gameStage;

			weight /= i.returnAmt;
			
			weight = 1 / weight;

			totalWeight += weight;

			decision += i.decision * weight;
		}
		decision /= totalWeight;
		
		parent.qPrint(name + ": I evaluate " + decision);
		
		keys.add(currentKey);
		activeKeys.add(currentKey);

		return decision >= 0 ? 0 : -1;
	}

	int getPlayerHash(LinkedList<PokerrPlayer> x) {
		int hash = 0;
		for (PokerrPlayer i : x) hash += Math.pow(10, parent.players.indexOf(i));
		return hash;
	}

	int playerHashDistance(int x, int y) {
		int dif = 0;
		for (int i = parent.players.size() - 1; i >= 0; i++) {
			int pow = (int) Math.pow(10, i);
			if (!((x > pow && y > pow) || (x < pow && y < pow))) dif++;
		}
		return dif;
	}

	@Override
	void winFdbk(boolean win, Card[] winningHand, int potAmt) {
		if (keys.getLast().decision == -1 
				&& parent.decideWinner(bestHand(parent.board,true),winningHand) == -1) {
			for (Key i : activeKeys) {
				i.returnAmt = startBank / 2;
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

	Key(Card[] holeCards, Card[] board, int betFacing, int playerHash, int gameStage, int returnAmt, int decision) {
		this.holeCards = holeCards;
		this.board = board;
		this.betFacing = betFacing;
		this.playerHash = playerHash;
		this.gameStage = gameStage;
		this.returnAmt = returnAmt;
		this.decision = decision;
		decided = false;
	}

	@Override
	public String toString() {
		StringBuilder toReturn = new StringBuilder("--Key--\n");
		toReturn.append("HoleCards: " + Arrays.toString(holeCards) + "\n");
		toReturn.append("Board: " + Arrays.toString(board) + "\n");
		toReturn.append("BetFacing: " + betFacing + "\n");
		toReturn.append("PlayerHash: " + playerHash + "\n");
		toReturn.append("BetFacing: " + betFacing + "\n");
		toReturn.append("GameStage: " + gameStage + "\n");
		toReturn.append("ReturnAmt: " + returnAmt + "\n");
		toReturn.append("Decision: " + decision + "\n");
		toReturn.append("Decided: " + decided + "\n");
		return toReturn.toString();
	}

}
