import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PokerrMain {


	final int ROUNDS = 10000;
	boolean PRINT = true;
	boolean SPEED = false;
	final int BB = 500;
	final int SB = BB / 2;
	boolean TEST = false;
	boolean CSV = true;



	LinkedList<PokerrPlayer> players = new LinkedList<PokerrPlayer>();
	int BBpos;
	int SBpos;
	Deck deck;
	Card[] board;
	int toPlay;
	boolean nextCard;
	int better;
	int gameStage;
	LinkedList<Pot> pots;
	int bankSum = 0;
	int iterations;
	int iterations2;
	int iterations3;

	final String[] gameIndex = new String[] {"PREFLOP","FLOP","TURN","RIVER","SHOWDOWN"};

	final String[] handIndex = new String[] {"HIGH CARD", "PAIR", "2 PAIR", "TRIPS", "STRAIGHT", "FLUSH", "FULL HOUSE", "FOUR OF A KIND", "STRAIGHT FLUSH", "ROYAL FLUSH"};

	PrintWriter out;

	StringBuilder globalString;

	PokerrMain() {
		globalString = new StringBuilder("");
	} // PokerrMain


	void definePlayers() {

		/*
		 * Folds ~1/11 of the time.
		 * Else, makes random move.
		 */
		addPlayer(true, "Dornk", new PokerrPlayer() {
			@Override
			int evaluate(int betfacing, Card[] board) {
				int decision = (BB/5)*ThreadLocalRandom.current().nextInt(-1, 10);
				if (betfacing >= bank)
					return ThreadLocalRandom.current().nextInt(-1,1);
				//if (decision + betfacing > bank)
				//return bank - betfacing;
				if (decision - (betfacing/2) > 0 && decision - (betfacing/2) < betfacing)
					return 0;
				if (decision - (betfacing/2) <= 0)
					return -1;
				if (decision + betfacing < BB)
					return BB - betfacing;
				return decision;
			}
		}); // RandomDecisionMaker


		/*
		 * Always minbets if no bet is facing it.
		 * Never folds.	
		 */
		addPlayer(true,"OptyMB", new PokerrPlayer() {
			@Override
			public int evaluate(int betfacing, Card[] board) {
				if (betfacing > bank)
					return 0;
				int evaluation;
				if (betfacing == 0) {
					evaluation = BB;
				} else {
					evaluation = 0;
				}
				if (evaluation + betfacing > bank)
					return bank - (evaluation + betfacing);
				return evaluation;
			}
		}); // AlwaysMinBet


		/*
		 * Goes all-in if it has anything better than a high card.
		 * Folds otherwise (except pre-flop).
		 */
		addPlayer(false,"Alli_MK1", new PokerrPlayer() {
			@Override
			public int evaluate(int betfacing, Card[] board) {
				if (getGameStage(board) == 0) {
					return 0;
				} else {
					if (strength(bestHand(board, true))[0] > 0)
						return bank - betfacing > 0 ? bank - betfacing : 0;
						if (betfacing == 0)
							return 0;
						return -1;
				}
			}
		}); // StrategicAllIn	


		/*
		 * Goes all-in if it has anything better than a pair.
		 * Folds otherwise (except pre-flop).
		 */
		addPlayer(false,"Alli_MK2", new PokerrPlayer() {
			@Override
			public int evaluate(int betfacing, Card[] board) {
				if (getGameStage(board) == 0) {
					return 0;
				} else {
					if (strength(bestHand(board, true))[0] > 1)
						return bank - betfacing > 0 ? bank - betfacing : 0;
						if (betfacing == 0)
							return 0;
						return -1;
				}
			}
		});


		/*
		 * Goes all-in if it has anything better than a high card that's not on the board.
		 * Folds otherwise (except pre-flop).
		 */
		addPlayer(false,"Alli_MK3", new PokerrPlayer() {
			@Override
			public int evaluate(int betfacing, Card[] board) {
				if (getGameStage(board) == 0) {
					return 0;
				} else {
					if (strength(bestHand(board, true))[0] > 0 
							&& strength(bestHand(board, true))[0] > strength(bestHand(board, false))[0])
						return bank - betfacing > 0 ? bank - betfacing : 0;
						if (betfacing == 0)
							return 0;
						return -1;
				}
			}
		});

		/*
		 * Goes all-in if it has anything better than a pair that's not on the board.
		 * Folds otherwise (except pre-flop).
		 *
		addPlayer("Alli_MK4", new PokerrPlayer() {
			@Override
			public int evaluate(int betfacing, Card[] board) {
				if (getGameStage(board) == 0) {
					return 0;
				} else {
					if (strength(bestHand(board, true))[0] > 1 
							&& strength(bestHand(board, true))[0] > strength(bestHand(board, false))[0])
						return bank - betfacing > 0 ? bank - betfacing : 0;
						if (betfacing == 0)
							return 0;
						return -1;
				}
			}
		});*/

		/*
		 * 
		 */
		addPlayer(true,"Add'y", new PokerrPlayer() {
			LinkedList<int[]> keys = new LinkedList<int[]>();
			double decision;
			@Override
			public int evaluate(int betfacing, Card[] board) {
				if (getGameStage(board) == 0)
					return 0;
				if (getGameStage(board) > 1)
					return (int) (betfacing == 0 ? BB : decision);
				
				
				// betfacing, strength, decision, outcome
				int[] bestKey = new int[]{500,0,0,1,0};
				// arbitrarily high to ensure is replaced
				int bestEval = 99999;

				if (keys.size() == 0)
					keys.add(bestKey);

				double toReturn = 0;			
				int count = 0;

				int[] strength = strength(bestHand(board,true));
				int[] strength2 = strength(bestHand(board,false));
				int val = strength[0];
				int val2 = strength2[0];
				int val3 = strength[2] - strength2[2];

				for (int[] key : keys) {
					int eval = (int) (0.5*Math.pow(key[0] - betfacing,2) 
							+ (10*BB*Math.pow(key[1] - (val-val2),2))
							+ (10*BB*Math.pow(key[4] - (val3),2)));
					if (eval < bestEval) {
						bestEval = eval;
						bestKey = key;
						toReturn = bestKey[2];
						count = 0;
					} else if (bestKey[0] == key[0] 
							&& bestKey[1] == key[1]
							&& bestKey[4] == key[4]) {
						if (key[3] == 1) {
							toReturn = (toReturn + key[2]) / 2;
						} else if (key[2] == 0) {
							toReturn = (toReturn + -1) / 2;
						} else {
							toReturn = (toReturn + 0) / 2;
						}
						count++;

					}
				}
				if (count < 500/(val-val2+1)) {
					toReturn = 0;
					qPrint("testing the waters...");
				}
				qPrint(Double.toString(toReturn));
				decision = (int) Math.round(toReturn);
				keys.add(new int[] {betfacing,val-val2,(int)Math.round(decision),0,val3});
				qPrint(Arrays.toString(bestKey));
				qPrint(Arrays.toString(keys.getLast()));
				return (int) (betfacing == 0 ? BB : decision);
			}
			@Override
			void winFdbk(boolean win) {
				if (win) {
					keys.getLast()[3] = 1;
				}
			}
		});
	}

	void qPrint(String x) {
		globalString.append(x + "\n");
	}

	void qPrintNN(String x) {
		globalString.append(x);
	}

	void addPlayer(boolean toggle, String y, PokerrPlayer x) {
		if (toggle) {
			x.name = y;
			bankSum += x.bank;
			players.add(x);
		}
	} // addPlayer

	void test() {
		PokerrPlayer firstPlayer = players.get(0);
		firstPlayer.holeCards[0] = new Card(14,1);
		firstPlayer.holeCards[1] = new Card(14,2);
		board = new Card[5];
		board[0] = new Card(2,3);
		board[1] = new Card(3,4);
		board[2] = new Card(2,1);
		board[3] = new Card(14,3);
		board[4] = new Card(5,3);
		if (firstPlayer.strength(firstPlayer.bestHand(board, true))[0] != 6) {
			qPrint(getBoard() + "\n" 
					+ Arrays.toString(firstPlayer.holeCards) + "\n" 
					+ Arrays.toString(firstPlayer.bestHand(board, true)));
			throw new RuntimeException("full house");
		}
		if (firstPlayer.strength(firstPlayer.bestHand(board, false))[0] != 1) {
			qPrint(firstPlayer.strength(board)[0]);
			throw new RuntimeException("board strength");
		}
	}

	void qPrint(int i) {
		globalString.append(i + "\n");
	}


	void printCSV() {
		String string = "";
		int potTotal = 0;
		for (Pot p : pots)
			potTotal += p.potAmt;
		string += iterations + "," 
				+ iterations2 + "," 
				+ iterations3 + "," 
				+ gameStage + "," 
				+ potTotal + ",";
		for (PokerrPlayer p : players) {
			if (p.bank != 0)
				string += p.bank;
			string += ",";
			string += p.totalWinnings+ ",";
			//if (gameStage == 4 && p.holeCards[0] != null) {
			//	string += p.strength(p.bestHand(board, true))[0] + ",";
			//} else { string += ","; }
		}
		string += activePlayers().size();
		out.println(string);
	}

	void start() {

		long time1 = System.currentTimeMillis();

		iterations = 0;
		definePlayers();

		File file = new File("pokercsv.csv");
		file.delete();
		try {
			file.createNewFile();
			String header = "";
			header += "game,hand,turn,gameStage,potAmt,";
			for (PokerrPlayer p : players) {
				header += p.name + "_bank,";
				header += p.name + "_winnings,";
				//header += p.name + "_strength,";
			}
			header += "playerSize";
			FileWriter fw = new FileWriter("pokercsv.csv", true);
			BufferedWriter bw = new BufferedWriter(fw);
			out = new PrintWriter(bw);
			out.println(header);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}




		if (TEST)
			test();		

		for (int k = 0; k < ROUNDS; k++) {
			iterations2 = 0;
			iterations++;
			for (PokerrPlayer p : players) {
				p.bank = p.STARTING_BANK;
				p.inTheHand = true;
			}
			SBpos = nextPlayer(-1);
			BBpos = nextPlayer(SBpos);
			pots = new LinkedList<Pot>();
			while (iterations2 < 10000) {
				iterations3 = 0;
				board = new Card[5];
				iterations2++;
				pots.clear();
				pots.add(new Pot(activePlayers(), 0, players.size()));

				deck = new Deck();
				gameStage = 0;
				toPlay = nextPlayer(BBpos);
				for (PokerrPlayer p : activePlayers()) {
					p.holeCards[0] = deck.randomCard();
					p.holeCards[1] = deck.randomCard();
					if (PRINT)
						qPrint(p.name + "(" + players.indexOf(p) + ")" + " is dealt " + p.holeCards[0] + "|" + p.holeCards[1]);
				}
				if (PRINT)
					qPrint("");

				nextCard = false;

				if (PRINT)
					qPrint(players.get(SBpos).name + "(" + SBpos + ")" + " puts in SB (" + SB + ")");
				toPlay = SBpos;
				playerAction(SB);
				if (PRINT)
					qPrint(players.get(BBpos).name + "(" + BBpos + ")" + " puts in BB (" + BB + ")");
				toPlay = BBpos;
				int totalBets = 0;
				for (Pot p : pots) {
					totalBets += p.bet;
				}
				playerAction(BB - totalBets);
				toPlay = nextPlayer(toPlay);



				while (gameStage != 4) {
					evaluateBetter();
					while (!nextCard) {
						playerAction(-1);
						better--;
						toPlay = nextPlayer(toPlay);
						if (better == 0)
							nextCard = true;
					} // for
					nextCard();
					nextCard = false;
					for (Pot i : pots) {
						i.bet = 0;
						i.callersAmt = new int[players.size()];
					}
					if (activePlayers().size() != 1) {
						printDebug(0);
					} else {
						printDebug(3);
					}
					for (PokerrPlayer p : players) {
						p.frontMoney = 0;
						if (p.bank < 0)
							throw new RuntimeException("negative bank");
					}
					if (CSV)
						printCSV();
					iterations3++;
				}
				int showdownPotCount = 0;
				for (Pot i : pots) {
					if (i.potAmt > 0) {
						showdown(i.potAmt, i.players);
						showdownPotCount++;
					}
				}
				//if (showdownPotCount > 4)
				//throw new RuntimeException("piss");
				int sum = 0;
				for (PokerrPlayer p : players) {
					sum += p.bank;
				}
				while (sum != bankSum) {
					activePlayers().get(0).bank++;
					sum++;
				}

				printDebug(0);
				int bankSum2 = 0;
				for (PokerrPlayer p : players) {
					bankSum2 += p.bank;
					if (p.bank < 0)
						throw new RuntimeException("negative bank");
				}
				if (bankSum != bankSum2)
					throw new RuntimeException("inflation/deflation");

				for (PokerrPlayer p : players) {
					p.holeCards[0] = null;
					p.holeCards[1] = null;
				}
				for (PokerrPlayer p : players) {
					p.inTheHand = p.bank > 0 ? true : false;
				}

				if (activePlayers().size() == 1) {
					activePlayers().get(0).totalWinnings += activePlayers().get(0).bank / 10000;
					qPrint("---------------------");
					qPrint("====" + iterations + "." + iterations2  + "====");
					printDebug(2);
					System.out.println(globalString);
					globalString = new StringBuilder("");
					break;
				}
				SBpos = nextPlayer(SBpos);
				BBpos = nextPlayer(SBpos);
				if (PRINT)
					qPrint("====" + iterations + "." + iterations2  + "====");

				System.out.println(globalString);
				globalString = new StringBuilder("");
			} // for
		} // for

		System.out.println(System.currentTimeMillis() - time1);

	} // start

	LinkedList<PokerrPlayer> activePlayers() {
		LinkedList<PokerrPlayer> activePlayers = new LinkedList<PokerrPlayer>();
		for(PokerrPlayer p : players) {
			if (p.inTheHand)
				activePlayers.add(p);
		}
		return activePlayers;
	} // activePlayers

	void evaluateBetter() {
		better = 0;
		for (PokerrPlayer p : players) {
			if (p.inTheHand)
				better++;
		} // for*
	} // evaluateBetter

	void showdown(int pot, LinkedList<PokerrPlayer> activePlayers) {
		PokerrPlayer winner = activePlayers.get(0);
		for(PokerrPlayer p : activePlayers) {
			Card[] winnerBestHand = winner.bestHand(board, true);
			Card[] pBestHand = p.bestHand(board, true);
			if (p.strength(pBestHand)[0] > winner.strength(winnerBestHand)[0]) {
				winner = p;
			} else if (p.strength(pBestHand)[0] == winner.strength(winnerBestHand)[0] && p != winner) {
				if (winner.strength(winnerBestHand)[2] < p.strength(pBestHand)[2]) {
					winner = p;
				} else if (winner.strength(winnerBestHand)[2] == p.strength(pBestHand)[2]){

					if (winner.strength(winnerBestHand)[3] < p.strength(pBestHand)[3]) {
						winner = p;
					} else {
						Integer[] winnerVals = new Integer[] {
								winnerBestHand[0].value,
								winnerBestHand[1].value,
								winnerBestHand[2].value,
								winnerBestHand[3].value,
								winnerBestHand[4].value
						};
						Integer[] pVals = new Integer[] {
								pBestHand[0].value,
								pBestHand[1].value,
								pBestHand[2].value,
								pBestHand[3].value,
								pBestHand[4].value
						};

						Arrays.sort(winnerVals, Collections.reverseOrder());
						Arrays.sort(pVals, Collections.reverseOrder());

						for (int i = 0; i < 5; i++) {
							if (winnerVals[i] < pVals[i]) {
								winner = p;
								break;
							} // if	
							if (winnerVals[i] > pVals[i]) {
								break;
							} // if
						} // for
					}
				} // if
			} // if
		} // for

		LinkedList<PokerrPlayer> winners = new LinkedList<PokerrPlayer>();
		winners.add(winner);
		for(PokerrPlayer p : activePlayers) {
			Card[] winnerBestHand = winner.bestHand(board, true);
			Integer[] winnerVals = new Integer[] {
					winnerBestHand[0].value,
					winnerBestHand[1].value,
					winnerBestHand[2].value,
					winnerBestHand[3].value,
					winnerBestHand[4].value
			};
			Arrays.sort(winnerVals, Collections.reverseOrder());

			Card[] pBestHand = p.bestHand(board, true);
			Integer[] pVals = new Integer[] {
					pBestHand[0].value,
					pBestHand[1].value,
					pBestHand[2].value,
					pBestHand[3].value,
					pBestHand[4].value
			};
			Arrays.sort(pVals, Collections.reverseOrder());

			boolean equ = true;
			for (int i = 0; i < 5; i++) {
				if (winnerVals[i] != pVals[i]) {
					equ = false;
				}
			} // for
			if (equ && p != winner) {
				winners.add(p);
			}
		}

		if (winners.size() == 1) {
			if (PRINT)
				qPrint("$$$ " + winner.name + "(" + players.indexOf(winner) + ")" + " wins " + pot + " $$$");
			winner.bank += pot;
			winner.winFdbk(true);
		} else {
			int counter = 0;
			for(PokerrPlayer p : winners) {
				counter++;
			}
			for(PokerrPlayer p : winners) {
				if (PRINT)
					qPrint("$$$ " + p.name + "(" + players.indexOf(p) + ")" +  " wins " + (pot / counter) + " $$$");
				p.bank += Math.round(pot / counter);
				p.winFdbk(true);
			}


		}

		if (PRINT)
			qPrint("");
	} // showdown

	void playerAction(int forceEval) {
		PokerrPlayer current = players.get(toPlay);
		boolean solo = true;
		for (PokerrPlayer p : players) {
			if (p.inTheHand && p != current)
				solo = false;
		}
		boolean splitPot = false;

		int bet = 0;
		int globalFrontMoney = 0;
		for (Pot i : pots) {
			globalFrontMoney += i.callersAmt[players.indexOf(current)];
			bet += i.bet;
		}

		int evaluation = 0;
		evaluation = forceEval == -1 ? current.evaluate(bet, board) : forceEval;

		if (PRINT)
			qPrintNN(current.name + "(" + players.indexOf(current) + ")" + " evaluates: " + evaluation);

		//sanitize input
		if (forceEval == -1 
				&& !(evaluation < 0)
				&& bet + evaluation - globalFrontMoney > current.bank 
				&& (current.bank - bet + globalFrontMoney > 0))
			evaluation = current.bank - bet + globalFrontMoney;
		if (forceEval == -1 
				&& !(evaluation < 0)
				&& bet + evaluation - globalFrontMoney > current.bank 
				&& (current.bank - bet + globalFrontMoney < 0))
			evaluation = 0;
		if (bet - globalFrontMoney == 0 && activePlayers().size() == 1)
			forceEval = -2;
		if (evaluation <= -1 && bet == 0)
			evaluation = 0;

		if (PRINT)
			qPrint(" (" + evaluation + ")");

		if (forceEval == -2) {
			if (PRINT)
				qPrint(current.name + "(" + players.indexOf(current) + ")" + " is the last remaining...");
			for (Pot i : pots) {
				if (i.players.size() != 0 && i.players.size() == 1 && i.potAmt != 0) {
					PokerrPlayer tempPlayer = i.players.get(0);
					tempPlayer.bank += i.potAmt;
					if (PRINT)
						qPrint(tempPlayer.name + "(" + players.indexOf(tempPlayer) + ")" + " takes their " + i.potAmt + " back");
					i.potAmt = 0;
					i.players.remove(tempPlayer);
					tempPlayer.winFdbk(true);
				}
			}
		} else {
			//fold
			if (evaluation < 0) {
				if (PRINT)
					qPrint(current.name + "(" + players.indexOf(current) + ")" + " folds");
				for (Pot i : pots) {
					if (i.players.size() != 0 && i.players.get(0) == current && i.players.size() == 1) {
						current.bank += i.potAmt;
						pots.remove(i);
						break;
					} else {
						i.players.remove(current);
					}
				}
				if (!solo)
					current.inTheHand = false;
				//call/raise
			} else {

				//error checking
				if (!(evaluation == 0) && bet + evaluation < bet * 2 && !(bet + evaluation == current.bank + globalFrontMoney))
					throw new RuntimeException("bet + eval < bet * 2");
				if (forceEval == -1 && evaluation != 0 && bet + evaluation > current.bank + globalFrontMoney)
					throw new RuntimeException("bet + eval > bank + globalFrontMoney");
				if (current.bank < 0)
					throw new RuntimeException("bank < 0");
				if (!(evaluation == 0) && bet + evaluation < BB && forceEval != SB && !(bet + evaluation == current.bank + globalFrontMoney)) {
					throw new RuntimeException("bet < BB");
				}

				int frontMoney = 0;
				pots.getFirst().bet += evaluation;
				if (bet + evaluation - globalFrontMoney > current.bank) {
					splitPot = true;
					for (int i = pots.size() - 1; i > -1; i--) {
						frontMoney += pots.get(i).callersAmt[players.indexOf(current)];
						if (pots.get(i).bet > current.bank + pots.get(i).callersAmt[players.indexOf(current)]) {
							Pot oldPot = pots.get(i);
							current.bank += oldPot.callersAmt[players.indexOf(current)];
							oldPot.potAmt -= oldPot.callersAmt[players.indexOf(current)];
							oldPot.callersAmt[players.indexOf(current)] = 0;
							Pot newPot = new Pot(new LinkedList<PokerrPlayer>(), 0, players.size());
							pots.add(pots.indexOf(oldPot), newPot);
							oldPot.callersAmt[players.indexOf(current)] = current.bank;
							newPot.bet = oldPot.bet - current.bank;
							oldPot.bet = current.bank;
							newPot.players = new LinkedList<PokerrPlayer>();//new LinkedList<PokerrPlayer>(new ArrayList<PokerrPlayer>(oldPot.players));
							//newPot.players.add(current);
							//newPot.players.remove(current);
							for (PokerrPlayer p : oldPot.players) {
								if (p != current && oldPot.callersAmt[players.indexOf(p)] > current.bank) {

									newPot.callersAmt[players.indexOf(p)] = oldPot.callersAmt[players.indexOf(p)] - current.bank;
									oldPot.callersAmt[players.indexOf(p)] -= newPot.callersAmt[players.indexOf(p)];

									newPot.potAmt += newPot.callersAmt[players.indexOf(p)];
									oldPot.potAmt -= newPot.callersAmt[players.indexOf(p)];
									newPot.players.add(p);
								}
							}
							//copypasted calling code
							oldPot.potAmt += current.bank;
							current.bank = 0;
							oldPot.callersAmt[players.indexOf(current)] = oldPot.bet;
						} else {
							Pot tempPot = pots.get(i);
							tempPot.potAmt += tempPot.bet - tempPot.callersAmt[players.indexOf(current)];
							current.bank -= tempPot.bet - tempPot.callersAmt[players.indexOf(current)];
							tempPot.callersAmt[players.indexOf(current)] = tempPot.bet;
							current.frontMoney += tempPot.bet;
							if (tempPot.potAmt != 0 && !tempPot.players.contains(current))
								tempPot.players.add(current);
						}
					}
					if (!solo)
						current.inTheHand = false;
				} else {
					for (Pot i : pots) {
						frontMoney += i.callersAmt[players.indexOf(current)];
						i.potAmt += i.bet - i.callersAmt[players.indexOf(current)];
						current.bank -= i.bet - i.callersAmt[players.indexOf(current)];
						i.callersAmt[players.indexOf(current)] = i.bet;
						current.frontMoney += i.bet;
						if (i.potAmt != 0 &&!i.players.contains(current))
							i.players.add(current);
					}
				}

				//prints
				if (PRINT) {
					if (solo)
						qPrint(current.name + "(" + players.indexOf(current) + ")" + " is the last one remaining");
					if (evaluation > 0) {
						qPrint(current.name + "(" + players.indexOf(current) + ")" + " raises to " + (bet + evaluation));
					} else if (bet - frontMoney == 0) {
						qPrint(current.name + "(" + players.indexOf(current) + ")" + " checks");
					} else {
						qPrint(current.name + "(" + players.indexOf(current) + ")" + " calls to a total of " 
								+ bet + " (an additional " + (bet  - frontMoney) + ")" );
					}
				}



				if (evaluation > 0)
					evaluateBetter();

				if (!splitPot && current.bank == 0) {
					if (!solo)
						current.inTheHand = false;
					if (pots.get(0).bet != 0)
						pots.add(0, new Pot(new LinkedList<PokerrPlayer>(), 0, players.size()));
				}
			}
		}
		if (PRINT)
			printDebug(1);
		//if (PRINT)
		//qPrint("");
	}

	void nextCard() {
		if (gameStage == 0) {

			if (PRINT)
				qPrint("-----^" + gameIndex[gameStage] + "^-----");
			for (int i = 0; i < 3; i++) {
				board[i] = deck.randomCard();
			} // for
			gameStage++;
		} else if (gameStage < 3) {

			if (PRINT)
				qPrint("-----^" + gameIndex[gameStage] + "^-----");
			board[gameStage + 2] = deck.randomCard();
			gameStage++;
		} else if (gameStage == 3) {

			if (PRINT)
				qPrint("-----^" + gameIndex[gameStage] + "^-----");
			gameStage++;
		} // if
		toPlay = nextPlayer(SBpos - 1);

		if (PRINT)
			qPrint("");
	}

	int nextPlayer(int pos) {
		if (pos + 1 >= players.size()) {
			return nextPlayer(-1);
		} else {
			if (players.get(pos + 1).inTheHand) {
				return pos + 1;
			} else {
				return nextPlayer(pos + 1);
			}
		}
	}

	void printDebug(int mode) {
		//qPrint("----\\/DEBUG\\/----");

		if (PRINT)
			qPrint("");
		if (mode == 0 && PRINT && !SPEED) {
			/*int sum = 0;
			int counter = 0;
			for (Pot i : pots) {
				if (i.potAmt != 0) {
					qPrint("Pot " + counter + ": " + i.potAmt);
					String playersPlayingForIt = "( ";	
					for (PokerrPlayer p : i.players) {
						playersPlayingForIt += p.name + "(" + players.indexOf(p) + ")" + " ";
					}
					playersPlayingForIt += ")";
					qPrint(playersPlayingForIt);
					sum += i.potAmt;
					counter++;
				}
			}
			qPrint("Pot total:  " + sum);*/
			printDebug(1);
			qPrint("Board:  " + getBoard() + "\n");
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).holeCards[0] == null) {
					qPrint(players.get(i).name + "(" + i + ")" + " was not dealt in this hand.");
				} else {
					PokerrPlayer p = players.get(i);
					Card[] pBestHand = p.bestHand(board, true);
					qPrint(p.name + "(" + i + ")" + " cards:  " + p.holeCards[0] + "|" + p.holeCards[1]
							+ " (besthand: " + Arrays.toString(pBestHand) + ")");
					qPrint(p.name + "(" + i + ")" + " strength:  " + p.strength(pBestHand)[0] 
							+ "." + p.strength(pBestHand)[1] 
									+ "." + p.strength(pBestHand)[2]
											+ "." + p.strength(pBestHand)[3]);
					qPrint(p.name + "(" + i + ")" + " bank:  " + p.bank);
					qPrint(p.name + "(" + i + ")" + " in the hand: " + p.inTheHand);
				}	
				qPrint("");
			} // for
		} // if
		if (mode == 1 && PRINT) {
			int sum = 0;
			int counter = 0;
			for (Pot i : pots) {
				qPrint("Pot " + counter + ": " + i.potAmt);
				qPrintNN(Arrays.toString(i.callersAmt));
				String playersPlayingForIt = "( ";	
				for (PokerrPlayer p : i.players) {
					playersPlayingForIt += p.name + "(" + players.indexOf(p) + ")" + " ";
				}
				playersPlayingForIt += ")";
				qPrint(playersPlayingForIt);
				sum += i.potAmt;
				counter++;
				qPrint("Bet: " + i.bet);
			}
			qPrint("Pot total:  " + sum);
		}
		if (mode == 2) {
			PokerrPlayer[] x = new PokerrPlayer[players.size()];
			for (PokerrPlayer p : players) {
				x[players.indexOf(p)] = p;
			}
			Arrays.sort(x, Comparator.comparingDouble(PokerrPlayer::getTotalWinnings));
			List<PokerrPlayer> y = Arrays.asList(x);
			Collections.reverse(y);
			x = y.toArray(new PokerrPlayer[0]);
			for (PokerrPlayer p : x) {
				qPrint(p.name + "(" + players.indexOf(p) + ")" + " winnings: " + p.totalWinnings);
			}
		}
		if (mode == 3 && PRINT && !SPEED) {
			qPrint("Board:  " + getBoard());
			int sum = 0;
			int counter = 0;
			for (Pot i : pots) {
				if (i.potAmt != 0) {
					qPrint("Pot " + counter + ": " + i.potAmt);
					String playersPlayingForIt = "( ";	
					for (PokerrPlayer p : i.players) {
						playersPlayingForIt += p.name + "(" + players.indexOf(p) + ")" + " ";
					}
					playersPlayingForIt += ")";
					qPrint(playersPlayingForIt);
					sum += i.potAmt;
					counter++;
				}
			}
			qPrint("Pot total:  " + sum);
		}
		//qPrint("----^DEBUG^----");
		if (PRINT)
			qPrint("");
	} // printDebug

	String getBoard() {
		String string = "[";
		for (int i = 0; i < board.length; i++) {
			if (board[i] != null) {
				string += board[i].toString();
			} else { string += " "; }
			if (i != board.length - 1)
				string += "|";
		}
		string += "]";
		return string;
	}

}
