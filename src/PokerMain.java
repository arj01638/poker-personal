import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PokerMain {


	final int ROUNDS = 10;
	final boolean PRINT = true;
	final boolean PRINT_MORE = false;
	final int BB = 500;
	final int SB = BB / 2;
	final int STARTING_BANK = 90*BB;
	final boolean TEST = true;
	final boolean CSV = false;
	final boolean STOP_WHEN_LAST_PLAYER_WINS = false;
	final int SLEEP = 0;



	final LinkedList<PokerPlayer> players = new LinkedList<>();
	int BBpos;
	int SBpos;
	Deck deck;
	Card[] board;
	int toPlay;
	boolean nextCard;
	int better;
	int gameStage;
	final LinkedList<Pot> pots = new LinkedList<>();
	int bankSum = 0;
	int game;
	int hand;
	int street;
	boolean endEarly = false;

	final String[] gameIndex = new String[] {"PREFLOP","FLOP","TURN","RIVER","SHOWDOWN"};

	final String[] handIndex = new String[] {"HIGH CARD", "PAIR", "2 PAIR", "TRIPS", "STRAIGHT", "FLUSH", "FULL HOUSE", "FOUR OF A KIND", "STRAIGHT FLUSH", "ROYAL FLUSH"};

	PrintWriter out;

	StringBuilder globalString;

	PokerMain() {
		globalString = new StringBuilder();
	} // PokerMain

	void slumber() {
		if (SLEEP > 0) {
			try {
				Thread.sleep(SLEEP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	void definePlayers() {

		/*
		 * Calls preflop.
		 * Bets proportionally to its hand's strength.
		 * Folds if it isn't getting good 'value'.
		 */
		addPlayer(true, "Val", new PokerPlayer(this) {
			@Override
			int evaluate() {
				if (getGameStage(board) == 0) return 0;
				int[] strength = strength(bh);
				int decision = (int) ((BB * Math.pow(((double)strength[0]+(double)strength[2]/14.0),2))/3.0);
				decision = sanitizeBetV(decision);
				return decision;
			}
		});

		/*
		 * Calculates
		 */
		addPlayer(false, "The Equator", new PokerPlayer(this) {
			@Override
			int evaluate() {
				//todo
				return 0;
			}
		});


		/*
		 * Folds ~1/11 of the time.
		 * Else, makes random move.
		 * After 50 hands, gets bored and bets progressively more while never folding.
		 */
		addPlayer(false, "Newbie", new PokerPlayer(this) {
			@Override
			int evaluate() {
				if (parent.hand > 50) return sanitizeBet(BB*((parent.hand-50)/5));
				int decision = (BB/5)*ThreadLocalRandom.current().nextInt(-1, 10);
				int betfacing = getBet();
				if (betfacing >= bank)
					return ThreadLocalRandom.current().nextInt(-1,1);
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
		 * Will min-bet if no bet is facing it about 20% of the time.
		 * Else, calls.
		 * Never folds.	
		 */
		addPlayer(true,"Calling Station", new PokerPlayer(this) {
			@Override
			public int evaluate() {
				int betFacing = getBet();
				if (betFacing > bank)
					return 0;
				int evaluation = (betFacing == 0 && Math.random() < 0.2) ? BB : 0;
				if (evaluation + betFacing > bank)
					return bank - (evaluation + betFacing);
				return evaluation;
			}
		});

		/*
		 * Goes all-in if it has anything better than a high card that's not on the board.
		 * Folds otherwise (except pre-flop).
		 */
		addPlayer(false,"Ally3", new PokerPlayer(this) {
			@Override
			public int evaluate() {
				int betfacing = getBet();
				if (getGameStage(board) == 0) {
					return 0;
				} else {
					if (strength(bh)[0] > 0
							&& strength(bh)[0] > strength(bestHand(board))[0])
						return Math.max(bank - betfacing, 0);
						if (betfacing == 0)
							return 0;
						return -1;
				}
			}
		});

		/*
		 * Only plays pocket pairs and broadway hands preflop.
		 * Bets proportionally (by BB) to hand strength if it has anything better than a high card that's not on the board.
		 * If facing a bet higher than the amount it wants to bet,
		 * by golly, it waited twenty orbits to get aces so it's gonna see a showdown.
		 */
		addPlayer(true,"The Nit", new PokerPlayer(this) {
			@Override
			public int evaluate() {
				int betFacing = getBet();
				if (getGameStage(board) == 0) {
					if (holeCards[0].val == holeCards[1].val
					|| (holeCards[0].val > 10 && holeCards[1].val > 10)) {
						return 0;
					} else { return -1; }
				} else {
					int[] strength = strength(bh);
					if (strength[0] > 0
							&& strength[0] > strength(bestHand(board))[0]) {
						if (betFacing == 0) return sanitizeBet(BB*(strength[0]+1));
						return 0;
					}
					if (betFacing == 0)
						return 0;
					return -1;
				}
			}
		});



		/* -------------------GRAVEYARD------------------- */


		/*
		 * Control, raises a random proportion of its bank.
		 * If can't raise that much, just call.
		 * For testing purposes.
		 */
		addPlayer(false,"Lucy", new PokerPlayer(this) {
			@Override
			public int evaluate() {
				int betfacing = getBet();
				if (betfacing > bank)
					return 0;
				int evaluation;
				evaluation = (int) (Math.random() * 0.25 * bank);

				evaluation = sanitizeBet(evaluation);

				return evaluation;
			}
		}); // control

		/*
		 * Goes all-in if it has anything better than a high card.
		 * Folds otherwise (except pre-flop).
		 */
		addPlayer(false,"Ally1", new PokerPlayer(this) {
			@Override
			public int evaluate() {
				int betfacing = getBet();
				if (getGameStage(board) == 0) {
					return 0;
				} else {
					if (strength(bh)[0] > 0)
						return Math.max(bank - betfacing, 0);
					if (betfacing == 0)
						return 0;
					return -1;
				}
			}
		});


		/*
		 * Goes all-in if it has anything better than a pair.
		 * Folds otherwise (except pre-flop).
		 */
		addPlayer(false,"Ally2", new PokerPlayer(this) {
			@Override
			public int evaluate() {
				int betfacing = getBet();
				if (getGameStage(board) == 0) {
					return 0;
				} else {
					if (strength(bh)[0] > 1)
						return Math.max(bank - betfacing, 0);
					if (betfacing == 0)
						return 0;
					return -1;
				}
			}
		});

		/*
		 * A mediocre poker bot.
		 */
		addPlayer(false,"Addy", new PokerPlayer(this) {
			//params

			//i will call if i haven't seen this scenario exactly countThreshold or more times.
			final int COUNT_THRESHOLD = 1;
			//print general debug info
			boolean debug = false;
			//step by step weighting process detailed :O
			boolean verboseDebug = false;
			//will switch on debug & verbose debug at this specified iteration
			final int START_DEBUG_ITERATION = Integer.MAX_VALUE; //ROUNDS - 5;
			//how many iterations between assess phases. "rounds" are 2x this number.
			final int ASSESS_INTERVAL = 300;

			// playerAmt, hand strength, board strength, gameStage, count, return, decision, outcome			
			LinkedList<int[]> keys = new LinkedList<>();
			int decision = 0;
			final int returnIndex = 5;
			final int decisionIndex = returnIndex + 1;
			final int outcomeIndex = decisionIndex + 1;
			final int countIndex = outcomeIndex + 1;
			int[] currentKey;
			int activeKeys = 0;
			int gameStage = 0;
			int startBank = bank;
			int startIteration = -1;
			int cIndex = 0;
			int cTotal = 0;
			Card[] bestHand;
			Card[] bestHandB;
			double evaluation;
			final String[] moveIndex = new String[] {"FOLD","PLAY"};

			//assess variables
			double lastWinnings = 0.0;
			final LinkedList<LinkedList<int[]>> masterKeys = new LinkedList<>();
			int keyIndex = 0;
			final LinkedList<Double> performanceVals = new LinkedList<>();
			boolean nextStage = false;



			LinkedList<int[]> getKeys() {
				LinkedList<int[]> toReturn = new LinkedList<>();
				if (!keys.isEmpty()) {
					for (int i = 0; i < activeKeys; i++) {	
						toReturn.add(keys.get( (keys.size() - 1) - i));
					}
				}
				return toReturn;
			}

			int getPlayerHash() {
				LinkedList<PokerPlayer> committedPlayers = committedPlayers();
				int hash = 0;
				for (PokerPlayer i : committedPlayers) {
					if (i != this)
						hash += Math.pow(players.indexOf(i) + 1, 2);
				}
				return hash;
			}

			void consolidate() {
				if (cIndex >= keys.size())
					cIndex = 0;
				int[] cKey = keys.get(cIndex);
				for (int[] i : keys) {
					if (i != cKey) {
						if (Arrays.equals(Arrays.copyOf(i, i.length - 1), Arrays.copyOf(cKey, cKey.length - 1))) {
							qPrint(name + ": consolidating " + Arrays.toString(i) + " and " + Arrays.toString(cKey));
							keys.remove(i);
							cKey[countIndex] += i[countIndex];
							cTotal++;
							qPrint(name + ": consolidated to " + Arrays.toString(cKey));
							break;
						}
					}
				}
				cIndex++;
				if (cIndex >= keys.size())
					cIndex = 0;
			}

			private <T> LinkedList<T> deepClone(LinkedList<T> x) {
				return new LinkedList<>(x);
			}

			void assessPerformance() {
				qPrint("");

				if (debug)
					for (LinkedList<int[]> i : masterKeys)
						qPrint(i.size());

				qPrint(name + ": Assessing performance...");
				masterKeys.add(deepClone(keys));
				performanceVals.add(totalWinnings - lastWinnings);
				qPrint(name + ": Current improvement: " + (totalWinnings - lastWinnings));
				qPrint(name + ": this set has length " + keys.size());

				if (!nextStage) {
					qPrint(name + ": Let's try out a new set of keys." );
					if (masterKeys.size() == 1) {
						keys = new LinkedList<>();
					} else { keys = deepClone(masterKeys.get(keyIndex)); }

					nextStage = true;
				} else {
					qPrint(name + ": Previous improvement: " + (performanceVals.get(performanceVals.size() - 2)));
					qPrintNN(name + ": This set is ");
					if (performanceVals.getLast() > performanceVals.get(performanceVals.size() - 2)) {
						keys = deepClone(masterKeys.getLast());
						qPrint("good, let's stick to it.");
					} else {
						keys = deepClone(masterKeys.get(masterKeys.size() - 2));
						qPrint("bad, let's go back...");
					}
					keyIndex = masterKeys.indexOf(keys);
					nextStage = false;
				}

				lastWinnings = totalWinnings;

				qPrint(name + ": this set has length " + keys.size());

				if (debug)
					for (LinkedList<int[]> i : masterKeys)
						qPrint(i.size());

				qPrint("");
			}

			@Override
			public int evaluate() {
				if (game >= START_DEBUG_ITERATION) {
					debug = true;
					verboseDebug = true;
				}
				gameStage = getGameStage(board);

				qPrint(name + ": stage:" + gameStage + "|" + "startIteration:" + startIteration + "|" + "currentIteration:" + hand);
				for (int[] i : getKeys()) 
					qPrint(name + ": getKeys:" + Arrays.toString(i));

				if (gameStage == 0 && startIteration != hand) {
					qPrint(name + ": Flushing systems...");

					if (game > 1 && game % ASSESS_INTERVAL == 0 && hand == 1) {
						for (int i = 0; i < 200; i++) consolidate();
						assessPerformance();
					}

					if (!keys.isEmpty() && keys.getLast()[outcomeIndex] == 0) {
						if (keys.getLast()[decisionIndex] == -1) {
							qPrint(name + ": Undecided keys are a loss.");
							if (evaluation < -0.9) {
								qPrint(name + ": ... *usually*, but this time I was pretty sure.");
								for (int[] i : getKeys()) {
									qPrint(name + ": Removing " + Arrays.toString(i));
									keys.remove(i);
								}
							} else {
								for (int[] i : getKeys()) {
									i[outcomeIndex] = -1;
									i[returnIndex] = Math.abs(bank - startBank);
								}
								for (int[] i : getKeys()) 
									qPrint(name + ": getKeys:" + Arrays.toString(i));
							}
						} else {
							if (bank > startBank) {
								qPrint(name + ": Scared 'em off!");
								winFdbk(true, null, 0);
							}
						}
					}		

					startBank = bank;
					activeKeys = 0;
					startIteration = hand;

					if (keys.size() != 0) {
						int cCount = 0;
						int initSize = keys.size();
						while (keys.size() != initSize - 1 && cCount < 4) {
							consolidate();
							cCount++;
						}
					}

					qPrint("");
				}

				if (committedPlayers().size() == 0 
						|| (committedPlayers().size() == 1 && committedPlayers().get(0) == this)
						|| (activePlayers().size() == 1 && getBet() == 0)) {
					qPrint(name + ": " + "Size is 0 or size is 1 and I'm last, checking.");
					if (bank > startBank)
						winFdbk(true, null, 0);
					return 0;
				}

				bestHand = bh;
				bestHandB = bestHand(board);

				int[] strengthHand = strength(bestHand);
				int[] strengthBoard = strength(bestHandB);
				currentKey = new int[] {
						committedPlayers().size(),
						(strengthHand[0] * 100) + strengthHand[2],
						gameStage != 0 ? (strengthBoard[0] * 100) + strengthBoard[2] : 0,
								gameStage,
								getPlayerHash(), // player "hash"
								0, // return
								1, // decision
								0, // outcome
								1, // count
				};

				if (verboseDebug) qPrint("currentKey: " + Arrays.toString(currentKey));

				if (keys.size() != 0 && currentKey[3] == keys.getLast()[3] && currentKey[outcomeIndex] == keys.getLast()[outcomeIndex]) {
					qPrint(name + ": re-evaluating " + Arrays.toString(keys.getLast()));
					keys.remove(keys.getLast());
					activeKeys--;
				}
				evaluation = 0;
				int count = 0;
				double weight;
				double totalWeight = 0;
				boolean shortCircuit = false;

				if (bank <= 2*BB) {
					if (gameStage == 0)
						evaluation = 1;
					else if (currentKey[1] - currentKey[2] < 100 && gameStage > 0) {
						evaluation = -1;
					} else if (gameStage > 0) {
						evaluation = 1;
					}
					shortCircuit = true;
				}

				if (shortCircuit) count = COUNT_THRESHOLD;

				if (!shortCircuit) {
					for (int[] i : keys) {
						for (int j = 0; j < i[countIndex]; j++) {
							if (i[3] != currentKey[3] 
									|| Math.abs(i[0] - currentKey[0]) > 1
									)//|| i[4] != currentKey[4]) 
								continue;
							if (verboseDebug) qPrint(Arrays.toString(i));
							weight = Math.pow((i[0] - currentKey[0]) * 35,4)
									+ Math.pow(i[1] - currentKey[1], 4)
									+ Math.pow(i[2] - currentKey[2], 4);

							if (verboseDebug) qPrint("Initial Sum: " + weight);

							weight *= .000002;

							if (verboseDebug) qPrint("*= .000002: " + weight);

							if (weight < 1)
								weight = 1;

							if (verboseDebug) qPrint("If < 1, =1: " + weight);

							if (Arrays.equals(Arrays.copyOf(i, returnIndex-1), Arrays.copyOf(currentKey, returnIndex-1))) {
								weight *= .75;
								count++;
							} // if

							if (verboseDebug) qPrint("If exact same: " + weight);

							weight *= Math.pow(1.0 - ((double)i[returnIndex] / ((double)players.size() * (double)STARTING_BANK)),2);

							if (verboseDebug) qPrint("Return Weighting: " + weight);

							if (i[decisionIndex]*i[outcomeIndex] == 1 && getBet() <= BB)
								//&& ((double)getBet()/(double)bank) <= 1.0)
								weight *= (((double)getBet()/(double)bank)/2.0) + 0.5;

							if (verboseDebug) qPrint("If low bet, call weight: " + weight);

							if (gameStage > 0 
									&& (getBet() >= bank || getBet() > 4*BB) 
									&& i[decisionIndex]*i[outcomeIndex] == 1
									&& powerDifference(i[1], i[2]) < 1)
								weight *= 2 + ((double)getBet() / (double)bank);

							if (verboseDebug) qPrint("If big bet post flop&power dif<1, weigh against call: " + weight);

							if (gameStage > 0 
									&& (getBet() >= bank || getBet() > 4*BB) 
									&& i[decisionIndex]*i[outcomeIndex] == 1
									&& powerDifference(i[1], i[2]) < 2)
								weight *= 2 + ((double)getBet() / (double)bank);

							if (verboseDebug) qPrint("If big bet post flop&power dif<2, weigh against call: " + weight);

							if (i[4] != currentKey[4]) weight *= 5;
							if (verboseDebug) qPrint("If player hash != current: " + weight);							

							weight = 1 / weight;
							if (verboseDebug) qPrint("inverse: " + weight);
							totalWeight += weight;

							if (gameStage > 0 
									&& getBet() >= bank 
									&& powerDifference(i[1], i[2]) < 1 
									&& i[decisionIndex]*i[outcomeIndex] == 1)
								weight *= -1;
							if (verboseDebug) qPrint("if all in and power dif<1, flip call!: " + weight);

							evaluation += (i[decisionIndex] * i[outcomeIndex]) * weight;

							if (debug)
								qPrint(Arrays.toString(i) + " | " + weight + " | " + evaluation / totalWeight);
						}

					} // for
					evaluation /= totalWeight;
				} // if

				//if (iterations > 1000) count = COUNT_THRESHOLD;
				if (count < COUNT_THRESHOLD) evaluation = 1;

				decision = evaluation >= 0 ? 0 : -1;
				currentKey[decisionIndex] = evaluation >= 0 ? 1 : -1;

				int percent = 100;
				if (keys.size() != 0)
					percent = (keys.size() * 100) / (keys.size() + cTotal);

				qPrint(name + ": I evaluate " + evaluation + " (" + Arrays.toString(currentKey) + ").\n"
						+ name + ": Key size is " + keys.size() + " (consolidated to " + percent + "% of theoretical).");

				if (count < COUNT_THRESHOLD) qPrint("\n" + name + ": Hmm, haven't seen this too often.");

				if (shortCircuit) qPrint(name + ": Resorting to preprogrammed move...");

				qPrint("\n" + name + ": There are " +
						currentKey[0] + " players playing for the pot.\n" + name + ": I have a " +
						handIndex[strengthHand[0]] + " (" + strengthHand[2] +  ") and the board has a " + 
						handIndex[strengthBoard[0]] + " (" + strengthBoard[2] +  ") .\n" + name + ": We're on the " +
						gameIndex[gameStage] + ", so I will " +
						moveIndex[decision+1] + "\n");

				keys.add(currentKey);
				activeKeys++;

				if (decision != -1) scaleDecision(evaluation == 1.000 ? 0 : evaluation);

				return decision;
			}

			int powerDifference(int first, int second) {
				first /= 100;
				second /= 100;
				return first - second;
			}
			void scaleDecision(double evaluation) {
				double BBscaler = ((currentKey[1]-currentKey[2])/100.0);
				int scaleCap = (Math.max(bank, STARTING_BANK / 2)) / BB;
				BBscaler *= scaleCap / 9.0;
				BBscaler /= 1.75;
				decision = (int) BBscaler * BB;
				decision *= Math.pow(evaluation + 1, 3);
				if (decision + getBet() < BB)
					decision = BB - getBet();
				if (decision + getBet() < getBet() * 2)
					decision = 0;
				if (decision + getBet() > bank) {
					decision = bank - (decision + getBet());
					if (decision < 0) decision = 0;
				}
			}

			void updateKeyOutcomes(int x, int y) {
				int lastDecision = 0;
				for (int[] i : getKeys()) {
					if (y == 0) {
						if (lastDecision == 0) lastDecision = i[decisionIndex];
						if (lastDecision != i[decisionIndex]) x *= -1;
						i[outcomeIndex] = x;
						qPrint(name + ": That was a " + (x > 0 ? "GOOD" : "BAD") + " move when I " +
								moveIndex[(i[decisionIndex] > 0 ? 1 : 0)] + "ED on the " +
								gameIndex[i[3]] + " with a " +
								handIndex[i[1] / 100] + " (" + i[1] % 100 +  ")");
						if (lastDecision != i[decisionIndex]) x *= -1;
						//i[decisionIndex] *= 1; // change key to reflect iuefuinruifasrf
						if (lastDecision != i[decisionIndex]) i[returnIndex] = 0;
						qPrint(name + ": New key: " + Arrays.toString(i));
					}
					if (y > 0) y--;
				}
			} // updateKeyOutcomes

			@Override
			void winFdbk(boolean win, Card[] winningHand, int potAmt) {
				startIteration = 0;
				for (int[] i : getKeys())
					i[returnIndex] = Math.abs(bank - startBank);

				for (int[] i : getKeys()) {
					qPrint(name + ": getKeys with returns:" + Arrays.toString(i));
				}
				if (keys.getLast()[outcomeIndex] == 0) {
					if (win) { //good call
						updateKeyOutcomes(1,0);
					} else if (keys.getLast()[decisionIndex] == -1 
							&& decideWinner(bh,winningHand) == -1) { //good fold
						updateKeyOutcomes(1,0);
					} else {
						updateKeyOutcomes(-1,0);
					}
					for (int[] i : getKeys()) {
						if (potAmt != 0 && i[decisionIndex] == -1 && i[outcomeIndex] == -1)
							i[returnIndex] = potAmt;
					}
				}
			}
		});


	}

	void qPrint(String x) {
		globalString.append(x).append("\n");
	}

	void qPrintNN(String x) {
		globalString.append(x);
	}

	void addPlayer(boolean toggle, String y, PokerPlayer x) {
		if (toggle) {
			x.name = y;
			bankSum += x.bank;
			players.add(x);
		}
	} // addPlayer

	void qPrint(int i) {
		globalString.append(i).append("\n");
	}

	void printCSV() {
		StringBuilder string = new StringBuilder();
		int potTotal = 0;
		for (Pot p : pots)
			potTotal += p.potAmt;
		string.append(game).append(",").append(hand).append(",").append(street).append(",").append(gameStage).append(",").append(potTotal).append(",");
		for (PokerPlayer p : players) {
			string.append(p.bank);
			string.append(",");
			string.append(p.totalWinnings).append(",");
			string.append(p.winningsSlope()).append(",");
		}
		string.append(activePlayers().size());
		out.println(string);
	}

	void start() {
		long time1 = System.currentTimeMillis();

		game = 0;
		definePlayers();

		if (TEST) {
			unitTest();
			System.out.println(System.currentTimeMillis() - time1);
			return;
		}



		if (CSV) {
			File file = new File("pokercsv.csv");
			file.delete();
			try {
				file.createNewFile();
				StringBuilder header = new StringBuilder();
				header.append("game,hand,street,gameStage,potAmt,");
				for (PokerPlayer p : players) {
					header.append(p.name).append("_bank,");
					header.append(p.name).append("_winnings,");
					header.append(p.name).append("_slope,");
				}
				header.append("playerSize");
				FileWriter fw = new FileWriter("pokercsv.csv", true);
				BufferedWriter bw = new BufferedWriter(fw);
				out = new PrintWriter(bw);
				out.println(header);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		for (int k = 0; k < ROUNDS; k++) {
			hand = 0;
			game++;

			for (PokerPlayer p : players) {
				p.bank = p.STARTING_BANK;
				p.inTheHand = true;
			}
			SBpos = nextPlayer(-1);
			BBpos = nextPlayer(SBpos);
			pots.clear();
			while (hand < 10000) {
				if (PRINT)
					qPrint("==========Game:" + game + "==========Hand:" + hand + "==========");
				street = 0;
				board = new Card[5];
				hand++;
				pots.clear();
				pots.add(new Pot(activePlayers(), players.size()));

				deck = new Deck(true);
				gameStage = 0;
				if (PRINT) {
					qPrint("==================" + gameIndex[gameStage] + "=================\n");
				}
				toPlay = nextPlayer(BBpos);
				for (PokerPlayer p : activePlayers()) {
					p.holeCards[0] = deck.randomCard();
					p.holeCards[1] = deck.randomCard();
					if (p.holeCards[1].val > p.holeCards[0].val) {
						Card temp = p.holeCards[1];
						p.holeCards[1] = p.holeCards[0];
						p.holeCards[0] = temp;
					}
					if (PRINT) {
						qPrint(p.name + "(" + players.indexOf(p) + ")" + " is dealt " + p.holeCards[0] + "," + p.holeCards[1]);
					}
				}

				nextCard = false;

				if (PRINT)
					qPrint("\n" + players.get(SBpos).name + "(" + SBpos + ")" + " puts in SB (" + SB + ")");

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


				for (PokerPlayer p : players) {
					if (p.holeCards[0] != null) {
						p.bh = p.bestHand(board, p.holeCards);
					} else {
						p.bh = null;
					}
				}

				while (gameStage != 4) {
					if (PRINT && gameStage != 0)
						qPrint("==================" + gameIndex[gameStage] + "=================\n");
					evaluateBetter();
					while (!nextCard) {
						playerAction(-1);
						better--;
						toPlay = nextPlayer(toPlay);
						if (better == 0)
							nextCard = true;
					} // for
					if (PRINT)
						qPrint("\n==================" + gameIndex[gameStage] + "=================");
					nextCard();
					for (PokerPlayer p : players) {
						if (p.holeCards[0] != null)
							p.bh = p.bestHand(board, p.holeCards);
					}
					nextCard = false;
					for (Pot i : pots) {
						i.bet = 0;
						i.callersAmt = new int[players.size()];
					}
					for (PokerPlayer p : players) {
						p.frontMoney = 0;
						if (p.bank < 0)
							throw new RuntimeException("negative bank");
					}
					printSummary(0);
					if (CSV) printCSV();
					street++;
				}

				for (int j = pots.size() - 1; j >= 0; j--) {
					Pot i = pots.get(j);
					if (i.potAmt > 0 && i.players.size() != 0) {
						showdown(i.potAmt, i.players);
					}
				}


				int sum = 0;
				for (PokerPlayer p : players) {
					sum += p.bank;
				}
				while (sum < bankSum) {
					activePlayers().get(0).bank++;
					sum++;
				}
				while (sum > bankSum) {
					activePlayers().get(0).bank--;
					sum--;
				}

				printSummary(0);
				int bankSum2 = 0;
				for (PokerPlayer p : players) {
					bankSum2 += p.bank;
					if (p.bank < 0)
						throw new RuntimeException("negative bank");
				}
				if (bankSum != bankSum2)
					throw new RuntimeException("inflation/deflation");

				for (PokerPlayer p : players) {
					p.holeCards[0] = null;
					p.holeCards[1] = null;
				}
				for (PokerPlayer p : players) {
					p.inTheHand = p.bank > 0;
				}

				if (activePlayers().size() == 1) {
					PokerPlayer p = activePlayers().get(0);
					p.totalWinnings += p.bank / ((double) p.STARTING_BANK);
					if (PRINT)
						qPrint("==================" + gameIndex[gameStage] + "=================\n");
					if (PRINT || game == ROUNDS) {
						printSummary(2);
						printGlobalString();
					}
					break;
				}
				SBpos = nextPlayer(SBpos);
				BBpos = nextPlayer(SBpos);

				if (PRINT)
					printGlobalString();

				if (endEarly) k = ROUNDS;
			} // for
		} // for

		System.out.println(System.currentTimeMillis() - time1);

	} // start

	void printGlobalString() {
		if (SLEEP > 0) {
			String[] lines = globalString.toString().split("\\n");
			for (String s : lines) {
				System.out.println(s);
				if (s.compareTo("") != 0)
					slumber();

			}
		} else {
			System.out.println(globalString);
		}
		globalString = new StringBuilder();
	}

	LinkedList<PokerPlayer> activePlayers() {
		LinkedList<PokerPlayer> activePlayers = new LinkedList<>();
		for(PokerPlayer p : players) {
			if (p.inTheHand)
				activePlayers.add(p);
		}
		return activePlayers;
	} // activePlayers

	LinkedList<PokerPlayer> committedPlayers() {
		LinkedList<PokerPlayer> committedPlayers = new LinkedList<>();
		for(PokerPlayer p : players) {
			for (Pot po : pots)
				if (po.players.contains(p) && !committedPlayers.contains(p)) {
					committedPlayers.add(p);
					break;
				}
		}
		return committedPlayers;
	} // activePlayers

	void evaluateBetter() {
		better = 0;
		for (PokerPlayer p : players) {
			if (p.inTheHand)
				better++;
		} // for*
	} // evaluateBetter

	// returns 1 if first beats second,
	// returns -1 if second beats first,
	// returns 0 if equivalent in value
	int decideWinner(Card[] first, Card[] second) {
		int decision = 0;

		int[] strengthFirst = PokerPlayer.strength(first);
		int[] strengthSecond = PokerPlayer.strength(second);

		if (strengthSecond[0] > strengthFirst[0]) {
			decision = -1;
			return decision;
		}
		if (strengthSecond[0] < strengthFirst[0]) {
			decision = 1;
			return decision;
		}
		if (strengthFirst[2] < strengthSecond[2]) {
			decision = -1;
			return decision;
		}
		if (strengthFirst[2] > strengthSecond[2]) {
			decision = 1;
			return decision;
		}
		if (strengthFirst[3] < strengthSecond[3]) {
			decision = -1;
			return decision;
		}
		if (strengthFirst[3] > strengthSecond[3]) {
			decision = 1;
			return decision;
		}

		Integer[] winnerVals = new Integer[]{
				first[0].val,
				first[1].val,
				first[2].val,
				first[3].val,
				first[4].val
		};
		Integer[] pVals = new Integer[]{
				second[0].val,
				second[1].val,
				second[2].val,
				second[3].val,
				second[4].val
		};

		Arrays.sort(winnerVals);
		Arrays.sort(pVals);

		for (int i = 0; i < 5; i++) {
			if (winnerVals[i] < pVals[i]) {
				decision = -1;
				return decision;
			} // if
			if (winnerVals[i] > pVals[i]) {
				decision = 1;
				return decision;
			} // if
		} // for

		return decision;
	}

	void showdown(int pot, LinkedList<PokerPlayer> potPlayers) {
		if (potPlayers.size() == 0) throw new RuntimeException("zero players in pot");
		PokerPlayer winner = potPlayers.get(0);
		LinkedList<PokerPlayer> winners = new LinkedList<>();
		winners.add(winner);
		for (int i = 0; i < potPlayers.size(); i++) {
			PokerPlayer p = potPlayers.get(i);
			if (p == winner) continue;
			Card[] winnerBestHand = winner.bh;
			Card[] pBestHand = p.bh;
			int val = decideWinner(winnerBestHand, pBestHand);
			//qPrint(Arrays.toString(winnerBestHand) + Arrays.toString(pBestHand) + val);
			if (val > 0) continue;
			if (val == 0) winners.add(p);
			if (val < 0) {
				winners.clear();
				winner = p;
				winners.add(p);
				i = 0;
			}
		} // for

		if (winners.size() == 1) {
			if (PRINT)
				qPrint("$$$$$$ " + winner.name + "(" + players.indexOf(winner) + ")" + " wins " + pot + " $$$$$$");
			winner.bank += pot;
			winner.winFdbk(true, null,pot);
			for (PokerPlayer p : players) {
				if (p != winner) {
					p.winFdbk(false,winner.bh,pot);
				}
			}
		} else {
			qPrint("e");
			int counter = winners.size();
			for(PokerPlayer p : winners) {
				if (PRINT)
					qPrint("$$$$$$ " + p.name + "(" + players.indexOf(p) + ")" +  " wins " + (pot / counter) + " $$$$$$");
				p.bank += Math.round((double)pot / (double)counter);
				p.winFdbk(true, null, pot / counter);
			}
			for (PokerPlayer p : players) {
				if (!winners.contains(p)) {
					p.winFdbk(false,winner.bh, pot / counter);
				}
			}


		}

		if (PRINT)
			qPrint("");
	} // showdown

	void playerAction(int forceEval) {
		PokerPlayer current = players.get(toPlay);
		boolean solo = true;
		for (PokerPlayer p : players) {
			if (p.inTheHand && p != current) {
				solo = false;
				break;
			}
		}
		boolean splitPot = false;

		int bet = 0;
		int globalFrontMoney = 0;
		for (Pot i : pots) {
			globalFrontMoney += i.callersAmt[players.indexOf(current)];
			bet += i.bet;
		}
		if (bet - globalFrontMoney == 0 && activePlayers().size() == 1)
			forceEval = -2;

		int evaluation;
		evaluation = forceEval == -1 ? current.evaluate() : forceEval;

		if (PRINT_MORE && forceEval == -1)
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
		if (evaluation <= -1 && bet == 0)
			evaluation = 0;

		if (PRINT_MORE && forceEval == -1)
			qPrint(" (" + evaluation + ")");

		if (forceEval == -2) {
			if (PRINT)
				qPrint(current.name + "(" + players.indexOf(current) + ")" + " is the last remaining...");
			for (Pot i : pots) {
				if (i.players.size() == 1 && i.potAmt != 0) {
					PokerPlayer tempPlayer = i.players.get(0);
					tempPlayer.bank += i.potAmt;
					if (PRINT)
						qPrint(tempPlayer.name + "(" + players.indexOf(tempPlayer) + ")" + " takes their " + i.potAmt + " back");
					i.potAmt = 0;
					i.players.remove(tempPlayer);
				}
			}
			for (int i = 0; i < pots.size(); i++) {
				if (pots.get(i).potAmt == 0) {
					pots.remove(i);
					break;
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
				current.inTheHand = false;
				if (solo)
					pots.getLast().players.getFirst().inTheHand = true;

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
							Pot newPot = new Pot(new LinkedList<>(), players.size());
							pots.add(pots.indexOf(oldPot), newPot);
							oldPot.callersAmt[players.indexOf(current)] = current.bank;
							newPot.bet = oldPot.bet - current.bank;
							oldPot.bet = current.bank;
							newPot.players = new LinkedList<>();
							for (PokerPlayer p : oldPot.players) {
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
							potSplitHelper(current, tempPot);
						}
					}
					if (!solo)
						current.inTheHand = false;
				} else {
					for (Pot i : pots) {
						frontMoney += i.callersAmt[players.indexOf(current)];
						potSplitHelper(current, i);
					}
				}

				//prints
				if (PRINT && forceEval == -1) {
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
						pots.add(0, new Pot(new LinkedList<>(), players.size()));
				}
			}
		}
	}

	private void potSplitHelper(PokerPlayer current, Pot i) {
		i.potAmt += i.bet - i.callersAmt[players.indexOf(current)];
		current.bank -= i.bet - i.callersAmt[players.indexOf(current)];
		i.callersAmt[players.indexOf(current)] = i.bet;
		current.frontMoney += i.bet;
		if (i.potAmt != 0 &&!i.players.contains(current))
			i.players.add(current);
	}

	void nextCard() {
		if (gameStage == 0) {
			for (int i = 0; i < 3; i++) {
				board[i] = deck.randomCard();
			} // for
			gameStage++;
		} else if (gameStage < 3) {
			board[gameStage + 2] = deck.randomCard();
			gameStage++;
		} else if (gameStage == 3) {
			gameStage++;
		} // if
		toPlay = nextPlayer(SBpos - 1);
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

	void printSummary(int mode) {

		if (mode == 0 && PRINT) {
			qPrint("");
			printSummary(1);
			qPrint("Board: " + getBoard() + "\n");
			StringBuilder str = new StringBuilder();
			for (PokerPlayer player : players) {
				if (player.holeCards[0] == null) {
					str.append(player.fullName()).append(" was not dealt in this hand.\n");
				} else {
					Card[] pBestHand = player.bh;
					str.append(player.fullName()).append(" cards:  ")
							.append(player.holeCards[0]).append(",").append(player.holeCards[1]).append(" (besthand: ")
							.append(Arrays.toString(pBestHand)).append(")\n");
					str.append(player.fullName()).append(" strength: ").append(handIndex[PokerPlayer.strength(player.bh)[0]]).append("\n");
					str.append(player.fullName()).append(" bank:  ").append(player.bank);
					str.append("\n");
					str.append(player.fullName()).append(player.inTheHand ? " is" : " is not").append(" in the hand.");
					str.append("\n");
				}
			} // for
			qPrint(str.toString());
		} // if

		// POT INFO
		if (mode == 1 && PRINT) {
			int sum = 0;
			int counter = 1;
			for (Pot i : pots) {
				qPrint("Pot " + counter + " total: " + i.potAmt);
				qPrintNN("Players: ");
				for (PokerPlayer p : i.players) {
					qPrintNN(p.fullName() + " ");
				}
				qPrint("");
				sum += i.potAmt;
				counter++;
			}
			qPrint("Pots total:  " + sum);
		}
		if (mode == 2) {
			PokerPlayer[] x = new PokerPlayer[players.size()];
			for (PokerPlayer p : players) {
				x[players.indexOf(p)] = p;
			}
			Arrays.sort(x, Comparator.comparingDouble(PokerPlayer::getTotalWinnings));
			List<PokerPlayer> y = Arrays.asList(x);
			Collections.reverse(y);
			x = y.toArray(new PokerPlayer[0]);


			if (STOP_WHEN_LAST_PLAYER_WINS 
					&& game > 10
					&& x[0] == players.getLast()) endEarly = true;


			for (PokerPlayer p : x) {
				p.winningHistory.add(p.totalWinnings);
				qPrint(p.name + "(" + players.indexOf(p) + ")" + " winnings: " + p.totalWinnings
						+ " (ROI: " + (p.totalWinnings / game) + ")");
			}
		}
		if (PRINT)
			qPrint("");
	} // printDebug

	String getBoard() {
		StringBuilder string = new StringBuilder("[");
		for (int i = 0; i < board.length; i++) {
			if (board[i] != null) {
				if (i != 0) string.append(",");
				string.append(board[i].toString());
			}
		}
		string.append("]");
		return string.toString();
	}

	void unitTest() {
		PokerPlayer pl = players.get(0);
		pl.holeCards[0] = new Card(14,1);
		pl.holeCards[1] = new Card(14,2);
		/*pl.holeCards[0] = new Card(4,1);
		pl.holeCards[1] = new Card(14,2);
		Card[] board = new Card[5];
		board[0] = new Card(3,1);
		board[1] = new Card(3,2);
		board[2] = new Card(11,2);
		board[3] = new Card(3,3);
		board[4] = new Card(11,4);
		System.out.println(Arrays.toString(pl.bestHand(board, pl.holeCards)));
		if (pl.bestHand(board, pl.holeCards)[3].val != 11) throw new RuntimeException("penis");*/



		int samples = 10000;
		int[] arr = pl.getEquity(2,new Card[5], samples);
		System.out.println(Arrays.toString(arr));
		System.out.println(((double)arr[2]) / (double)(arr[0] + arr[1] + arr[2]) + "|" + (arr[0] + arr[1] + arr[2]));
	}


}
