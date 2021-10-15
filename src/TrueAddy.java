import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TrueAddy extends PokerrPlayer {

	boolean DEBUG = true;
	boolean vDEBUG = true;
	int TURN_ON_DEBUG_ROUND = -1;

	TrueAddy(PokerrMain parent) {
		super(parent);
	}

	LinkedList<Key> keys = new LinkedList<>();
	LinkedList<Key> activeKeys = new LinkedList<>();
	int startBank = 0;

	@Override
	int evaluate() {
		if (parent.iterations > TURN_ON_DEBUG_ROUND && TURN_ON_DEBUG_ROUND != -1) {
			DEBUG = true;
			vDEBUG = true;
		}

		if (getPlayerHash(parent.committedPlayers()).size() == 0) {
			parent.qPrint(name + ": No players...");
			return 0;
		}

		int gameStage = getGameStage(parent.board);

		if (gameStage == 0) {
			if (!keys.isEmpty() && !keys.getLast().decided) {
				parent.qPrint(name + "Last keys:");
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
			if (Math.abs(i.gameStage - currentKey.gameStage) > 1) continue;
			if (Math.abs(playerHashDistance(currentKey.playerHash, i.playerHash)) > 1) continue;
			if (!i.equals(currentKey)) continue;
			if (vDEBUG) parent.qPrint("" + i);

			double weight = 0.0;

			for (Card x : currentKey.holeCards) {
				for (Card y : i.holeCards) {
					weight += Math.pow((100.0/12.0)*Math.abs(x.compareTo(y)),2);
					weight += Math.pow((100.0/3.0)*Math.abs(x.compareToS(y)),2);
				}
			}
			for (Card x : currentKey.board) {
				if (x == null) continue;
				for (Card y : i.board) {
					if (y == null) continue;
					weight += Math.pow((100.0/12.0)*Math.abs(x.compareTo(y)),2);
					weight += Math.pow((100.0/3.0)*Math.abs(x.compareToS(y)),2);
				}
			}
			if (vDEBUG) parent.qPrint("Initial card weight: " + weight);


			weight += Math.pow(200.0*Math.abs(currentKey.board.size() - i.board.size()) ,2);
			weight += Math.pow((100.0/(parent.players.size()*STARTING_BANK))
					*Math.abs(currentKey.betFacing - i.betFacing),2);
			weight += Math.pow((100.0/parent.players.size())
					*Math.abs(playerHashDistance(currentKey.playerHash, i.playerHash)),2);

			weight += Math.pow((100.0/4.0)*(currentKey.gameStage - i.gameStage),2);
			if (vDEBUG) parent.qPrint("After other key vals: " + weight);

			if (currentKey.equals(i)) count++;
			if (vDEBUG) parent.qPrint("Seen this before.");
			if (weight < 1 && weight >= 0) weight++;
			if (weight > -1 && weight < 0) weight--;
			if (vDEBUG) parent.qPrint("Readjusting...: " + weight);
			weight = 1 / weight;
			if (vDEBUG) parent.qPrint("Inverting...: " + weight);

			weight *= (i.returnAmt >= 0 ? 1 : -1) + i.returnAmt/(parent.players.size()*STARTING_BANK);
			if (vDEBUG) parent.qPrint("Compensating for returnAmt: " + weight);

			totalWeight += Math.abs(weight);
			decision += i.decision * weight;
			avgBet += i.bet * weight;

			if (vDEBUG) parent.qPrint("Decision: " + (decision / totalWeight));

			if (Double.isNaN(decision / totalWeight)) {
				throw new RuntimeException("decision = NaN");
			}
			
			if (vDEBUG) parent.qPrint("\n\n");
		}
		decision /= totalWeight;
		avgBet /= totalWeight;

		if (keys.isEmpty() || totalWeight == 0) decision = 1;

		if (count == 0) {
			parent.qPrint(name + ": Haven't seen this before.");
			decision += 0.1;
			avgBet = Math.random()/4.0;
		} else {
			parent.qPrint(name + ": I've seen something similar.");
		}
		if (Double.isNaN(decision)) {
			throw new RuntimeException("decision == NaN");
		}
		parent.qPrint(name + ": I evaluate " + decision);

		int intDecision = decision >= 0 ? 0 : -1;
		currentKey.decision = intDecision == 0 ? 1 : -1;
		if (avgBet < 0) avgBet = 0;
		currentKey.bet = avgBet;
		if (vDEBUG) parent.qPrint("AvgBet: " + currentKey.bet);

		keys.add(currentKey);
		activeKeys.add(currentKey);

		//consolidate();
		
		int toReturn = intDecision;
		if (toReturn < 0) return -1;
		toReturn = (int) (currentKey.bet * bank);

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
	
	void consolidate() {
		int index = (int) Math.random() * (keys.size() - 1);
		Key j = keys.get(index);
		for (Key i : keys) {
			if (i != j && j.consolidatable(i)) {
				if (DEBUG) parent.qPrint("Consolidating...");
				if (DEBUG) parent.qPrint(""+i);
				if (DEBUG) parent.qPrint(""+j);
				j.count++;
				j.bet = (j.bet+i.bet)/2;
				j.returnAmt = (i.returnAmt+j.returnAmt)/2;
				if (DEBUG) parent.qPrint("New key:");
				if (DEBUG) parent.qPrint(""+j);
				keys.remove(i);
				break;
			}
		}
	}

	HashSet<PokerrPlayer> getPlayerHash(LinkedList<PokerrPlayer> x) {
		return new HashSet<>(x);
	}

	int playerHashDistance(HashSet<PokerrPlayer> x, HashSet<PokerrPlayer> y) {
		int difs = 0;
		for (PokerrPlayer p : x) {
			if (!y.contains(p)) difs++;
		}
		for (PokerrPlayer p : y) {
			if (!x.contains(p)) difs++;
		}
		return difs;
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
	HashSet<Card> holeCards;
	HashSet<Card> board;
	int betFacing;
	HashSet<PokerrPlayer> playerHash;
	int gameStage;
	int returnAmt;
	int decision;
	boolean decided;
	double bet;
	int count;

	PokerrPlayer parent;

	Key(PokerrPlayer parent, Card[] holeCards3, Card[] board3, int betFacing, HashSet<PokerrPlayer> playerHash, int gameStage, int returnAmt, int decision, double bet) {
		ArrayList<Card> holeCardList = new ArrayList<Card>();
		ArrayList<Card> boardList = new ArrayList<Card>();

		for (Card i : holeCards3)
			if (i != null) holeCardList.add(i);
		for (Card i : board3)
			if (i != null) boardList.add(i);
		
		this.holeCards = new HashSet<>(holeCardList);
		this.board = new HashSet<>(boardList);

		this.betFacing = betFacing;
		this.playerHash = playerHash;
		this.gameStage = gameStage;
		this.returnAmt = returnAmt;
		this.decision = decision;
		this.bet = bet;
		decided = false;

		this.parent = parent;
		
		count = 0;
	}

	@Override
	public String toString() {
		StringBuilder toReturn = new StringBuilder("--Key--\n");
		toReturn.append("HoleCards: " + holeCards + "\n");
		toReturn.append("Board: " + board + "\n");
		toReturn.append("BetFacing: " + betFacing + "\n");
		toReturn.append("PlayerHash: " + playerHash + "\n");
		toReturn.append("GameStage: " + parent.parent.gameIndex[gameStage] + "\n");
		toReturn.append("ReturnAmt: " + returnAmt + "\n");
		toReturn.append("Decision: " + decision + "\n");
		toReturn.append("Decided: " + decided + "\n");
		toReturn.append("Bet: " + bet + "\n");
		return toReturn.toString();
	}

	boolean equals(Key x) {
		boolean equalTo = false;
		int threshold = this.gameStage == 0 ? 3 : 8;
		if (cardDifs(x) < threshold &&
				Math.abs(this.betFacing - x.betFacing) < parent.BB &&
				this.playerHash.equals(x.playerHash) &&
				this.gameStage == x.gameStage) {
			equalTo = true;
		}
		return equalTo;
	}
	boolean consolidatable(Key x) {
		boolean equalTo = false;
		if (this.holeCards.equals(x.holeCards) &&
				this.board.equals(x.board) &&
				this.betFacing == x.betFacing &&
				this.playerHash.equals(x.playerHash) &&
				this.gameStage == x.gameStage &&
				this.decision == x.decision &&
				this.decided == x.decided) {
			equalTo = true;
		}
		return equalTo;
	}
	
	int cardDifs(Key other) {
		int difs = 0;
		for (Card x : this.holeCards) {
			for (Card y : other.holeCards) {
				difs += Math.abs(x.compareTo(y));
				difs += Math.abs(x.compareToS(y));
			}
		}
		for (Card x : this.board) {
			if (x == null) continue;
			for (Card y : other.board) {
				if (y == null) continue;
				difs += Math.abs(x.compareTo(y));
				difs += Math.abs(x.compareToS(y));
			}
		}
		return difs;
	}

}
