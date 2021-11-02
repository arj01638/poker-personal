import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TrueAddy2 extends PokerrPlayer {

	boolean DEBUG = true;
	boolean vDEBUG = false;
	int TURN_ON_DEBUG_ROUND = -1;
	double COUNT_THRESHOLD = 4;
	int EXP = 3;
	int LEARNING_KEYS = 50000;

	TrueAddy2(PokerrMain parent) {
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
			if (parent.iterations2 != 1 
					&& !keys.isEmpty() 
					&& !keys.getLast().decided) {
				parent.qPrint(name + ": Last keys:");
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
		//double avgBet = 0;
		int count = 0;
		for (Key i : keys) {
			if (keys.size() < LEARNING_KEYS) continue;
			if (activeKeys.contains(i)) continue;
			if (Math.abs(i.gameStage - currentKey.gameStage) > 1) continue;
			if (Math.abs(playerHashDistance(currentKey.playerHash, i.playerHash)) > 1) continue;
			if (i.gameStage != 0 && currentKey.gameStage == 0) continue;
			//if (!i.equals(currentKey)) continue;
			if (vDEBUG) parent.qPrint("" + i);

			double weight = 0.0;

			for (Card x : currentKey.board) {
				for (Card y : i.board) { 
					weight += Math.pow((100.0/12.0)*Math.abs(x.compareTo(y)),EXP);
					weight += Math.pow((100.0/36.0)*Math.abs(x.compareToS(y)),EXP);
				}
			}
			for (Card x : currentKey.holeCards) {
				for (Card y : i.holeCards) { 
					weight += Math.pow((100.0/12.0)*Math.abs(x.compareTo(y)),EXP);
					weight += Math.pow((100.0/36.0)*Math.abs(x.compareToS(y)),EXP);
				}
			}

			if (vDEBUG) parent.qPrint("Initial card weight: " + weight);


			//weight += Math.pow(200.0*Math.abs(currentKey.board.size() - i.board.size()) ,2);
			weight += Math.pow((100.0/(parent.players.size()*STARTING_BANK))
					*Math.abs(currentKey.betFacing - i.betFacing),EXP);
			weight += Math.pow((100.0/parent.players.size())
					*Math.abs(playerHashDistance(currentKey.playerHash, i.playerHash)),EXP);

			weight += Math.pow((100.0/4.0)*(currentKey.gameStage - i.gameStage),EXP);
			if (vDEBUG) parent.qPrint("After other key vals: " + weight);

			if (currentKey.equals(i)) count++;
			if (vDEBUG) parent.qPrint("Seen this before.");
			if (weight < 1 && weight >= 0) weight++;
			if (weight > -1 && weight < 0) weight--;
			if (vDEBUG) parent.qPrint("Readjusting...: " + weight);
			weight = 1 / weight;
			if (vDEBUG) parent.qPrint("Inverting...: " + weight);

			weight *= (i.returnAmt >= 0 ? 1 : -1) + Math.pow(((double)i.returnAmt/(double)(parent.players.size()*STARTING_BANK)),EXP);
			if (vDEBUG) parent.qPrint("Compensating for returnAmt: " + weight);

			totalWeight += Math.abs(weight);
			decision += i.returnAmt != 0 ? i.decision * weight : 0;

			if (vDEBUG) parent.qPrint("Decision: " + (decision / totalWeight));

			if (Double.isNaN(decision / totalWeight)) {
				throw new RuntimeException("decision = NaN");
			}

			if (vDEBUG) parent.qPrint("\n\n");
		}
		decision /= totalWeight;
		//avgBet /= totalWeight;

		if (keys.isEmpty() || totalWeight == 0) decision = 1;

		if (count <= 2) {
			parent.qPrint(name + ": Haven't seen this before.");
			decision += 0.1;
			//avgBet = Math.random()/4.0;
		} else {
			parent.qPrint(name + ": I've seen something similar.");
		}
		if (Double.isNaN(decision)) {
			throw new RuntimeException("decision == NaN");
		}
		parent.qPrint(name + ": I evaluate " + decision);
		if (keys.size() < LEARNING_KEYS) {
			parent.qPrint(name + ": Actually, let's see where this goes.");
			decision = 0;
		}

		int intDecision = decision >= 0 ? 0 : -1;
		currentKey.decision = intDecision == 0 ? 1 : -1;
		//if (avgBet < 0) avgBet = 0;
		//if (vDEBUG) parent.qPrint("AvgBet: " + currentKey.bet);

		keys.add(currentKey);
		activeKeys.add(currentKey);
		

		int toReturn = intDecision;
		if (toReturn < 0) return -1;
		toReturn = (int) ((decision-(count <= 2 ? 0 : 0.1)) * bank);

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
				&& keys.size() != 0
				&& holeCards[0] != null
				&& keys.getLast().decision == -1 
				&& parent.decideWinner(bestHand(parent.board,true),winningHand) == -1) {
			for (Key i : activeKeys) {
				i.returnAmt = startBank / 2;
				i.decided = true;
			}
		} 
		if (!keys.isEmpty() && !keys.getLast().decided) {
			parent.qPrint(name + ": Last keys:");
			for (Key i : activeKeys) {
				i.returnAmt = bank - startBank;
				i.decided = true;
				parent.qPrint(i.toString());
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
	int count;

	PokerrPlayer parent;

	Key(PokerrPlayer parent, Card[] holeCards, Card[] board, int betFacing, HashSet<PokerrPlayer> playerHash, int gameStage, int returnAmt, int decision, double bet) {
		ArrayList<Card> hc = new ArrayList<Card>();
		ArrayList<Card> b = new ArrayList<Card>();

		for (Card i : holeCards)
			if (i != null) hc.add(i);
		for (Card i : board)
			if (i != null) b.add(i);

		// todo, keep holecards and board hashsets, 
		this.holeCards = new HashSet<Card>(hc);
		this.board = new HashSet<Card>(b);


		this.betFacing = betFacing;
		this.playerHash = playerHash;
		this.gameStage = gameStage;
		this.returnAmt = returnAmt;
		this.decision = decision;
		decided = false;

		this.parent = parent;

		count = 0;
	}

	@Override
	public String toString() {
		StringBuilder toReturn = new StringBuilder("--Key--\n");
		toReturn.append("Hole Cards: " + holeCards + "\n");
		toReturn.append("Board: " + board + "\n");
		toReturn.append("BetFacing: " + betFacing + "\n");
		toReturn.append("PlayerHash: " + playerHash + "\n");
		toReturn.append("GameStage: " + parent.parent.gameIndex[gameStage] + "\n");
		toReturn.append("ReturnAmt: " + returnAmt + "\n");
		toReturn.append("Decision: " + decision + "\n");
		toReturn.append("Decided: " + decided + "\n");
		//toReturn.append("Bet: " + bet + "\n");
		return toReturn.toString();
	}

	boolean equals(Key x) {
		boolean equalTo = false;
		int threshold = this.gameStage == 0 ? 3 : 5;
		if (cardDifs(x) <= threshold &&
				Math.abs(this.betFacing - x.betFacing) < parent.BB &&
				this.playerHash.equals(x.playerHash) &&
				this.gameStage == x.gameStage) {
			equalTo = true;
		}
		return equalTo;
	}

	double cardDifs(Key other) {
		double difs = 0;
		for (Card x : this.board) {
			for (Card y : other.board) { 
				difs += Math.abs(x.compareTo(y));
				difs += 1/12*Math.abs(x.compareToS(y));
			}
		}
		return difs;
	}

}
