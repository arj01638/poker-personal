package pokerPersonal;

import java.util.Arrays;
import java.util.LinkedList;

public abstract class PokerPlayer {

    int BB = 500;
    int SB = BB / 2;
    int STARTING_BANK = 10000;
    public int bank;
    public final Card[] holeCards = new Card[2];
    public boolean inTheHand = true;
    public double totalWinnings = 0;
    public String name = "";
    public int frontMoney = 0;
    public final PokerMain parent;
    public final LinkedList<Double> winningHistory;
    public Card[] bh;

    PokerPlayer(PokerMain parent) {
        BB = parent.BB;
        SB = parent.SB;
        STARTING_BANK = parent.STARTING_BANK;
        this.parent = parent;
        bank = STARTING_BANK;
        winningHistory = new LinkedList<>();
    }

    public String fullName() {
        return name + " (" + parent.players.indexOf(this) + ")";
    }

    public double winningsSlope() {
        double sum = 0;
        int lookback = 150;
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
            int[] CEStrength = new int[]{0, 0, 0, 0};
            if (ce[0].val == ce[1].val)
                CEStrength[0] = 1;
            CEStrength[2] = Math.max(ce[0].val, ce[1].val);
            return CEStrength;
        }


        int[] CEStrength = new int[]{0, 0, 0, 0};
        CEStrength[2] = ce[0].val;

        //optimization: anything satisfying this condition can not make a hand better than high card
        if (ce[0].suit != ce[1].suit && !(ce[0].val == ce[1].val || ce[0].val == ce[1].val + 1)) return CEStrength;

        //pair
        if (ce[0].val == ce[1].val) {
            CEStrength[0] = 1;
            //two pair
            if (ce.length >= 4 && ce[3] != null && ce[2].val == ce[3].val) {
                CEStrength[0] = 2;
                CEStrength[2] = Math.max(ce[0].val, ce[2].val);
                CEStrength[3] = Math.min(ce[0].val, ce[2].val);
            }

            //trips
            if (ce[1].val == ce[2].val) {
                CEStrength[0] = 3;

                //full house
                if (ce.length >= 5 && ce[4] != null && ce[3].val == ce[4].val) {
                    CEStrength[0] = 6;
                    CEStrength[3] = ce[3].val;
                    return CEStrength;
                }

                //quads
                if (ce.length >= 4 && ce[3] != null && ce[2].val == ce[3].val) {
                    CEStrength[0] = 7;
                    return CEStrength;
                }
            }
        } // if

        if (ce.length >= 5 && ce[4] != null) {
            //straight
            if (ce[0].val == ce[1].val + 1
                    && ce[1].val == ce[2].val + 1
                    && ce[2].val == ce[3].val + 1
                    && ce[3].val == ce[4].val + 1) {
                CEStrength[0] = 4;
                //straight flush
                if (ce[0].suit == ce[1].suit && ce[1].suit == ce[2].suit && ce[2].suit == ce[3].suit && ce[3].suit == ce[4].suit) {
                    CEStrength[0] = 8;
                    if (ce[0].val == 14) {
                        CEStrength[0] = 9;
                    }
                    return CEStrength;
                }

                //wheel straight
            } else if (ce[0].val == 5
                    && ce[1].val == 4
                    && ce[2].val == 3
                    && ce[3].val == 2
                    && ce[4].val == 14) {
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
                    CEStrength[2] = ce[0].val;
                }
            }
        }

        return CEStrength;
    } // strength

    private Card[] bestHandHelper(Card[] raw, int count) {
        /* COMPUTE METRICS */
        int[] matches = new int[count];
        int[] suits = new int[4];
        boolean hasStraight = false;
        boolean hasFlush = false;
        //int flushSuit = -1;
        boolean[] straightIndex = new boolean[count];
        boolean[] flushIndex = new boolean[count];
        for (int i = 0; i < count; i++) {
            //suit tallying
            if (!hasFlush) {
                suits[raw[i].suit - 1]++;
                if (suits[raw[i].suit - 1] >= 5) {
                    hasFlush = true;
                    for (int j = 0; j < count; j++) {
                        if (raw[j].suit == raw[i].suit) {
                            flushIndex[j] = true;
                        }
                    }
                }
            }

            //match finding
            for (int j = 0; j < count; j++) {
                if (raw[i].val == raw[j].val) {
                    matches[i]++;
                }
            }

            //straight checking
            if (!hasStraight && i + 3 < count) {
                int index = raw[i].val;
                int straightCount = 1;
                straightIndex[i] = true;
                for (int j = i; j < count; j++) {
                    if (raw[j].val == index - 1) {
                        straightIndex[j] = true;
                        index--;
                        straightCount++;
                    } else if (raw[j].val == index) {
                        straightIndex[j] = true;
                    }
                    if (index == 2 && straightCount >= 4) {
                        for (int k = 0; k < count; k++) {
                            if (raw[k].val == 14) {
                                straightIndex[k] = true;
                                straightCount++;
                            }
                        }
                    }
                    if (straightCount >= 5) {
                        hasStraight = true;
                    }
                }
                if (!hasStraight) {
                    straightIndex = new boolean[count];
                }
            }
        }

        /* COMPUTE BEST STRENGTH */
        int strength = 0;

        int numFours = 0;
        int numThrees = 0;
        int numTwos = 0;
        boolean SFindex[] = new boolean[count];
        for (int i = 0; i < count; i++) {
            if (matches[i] == 4)
                numFours++;
            if (matches[i] == 3)
                numThrees++;
            if (matches[i] == 2)
                numTwos++;
        }
        //pair
        if (numTwos > 1) strength = 1;
        //two pair
        if (numTwos > 2) strength = 2;
        //trips
        if (numThrees > 0) strength = 3;
        //straight
        if (hasStraight) strength = 4;
        //flush
        if (hasFlush) strength = 5;
        //full house
        if ((numThrees > 0 && numTwos > 1) || numThrees > 3) strength = 6;
        //quads
        if (numFours > 0) strength = 7;
        //straight flush
        if (hasStraight && hasFlush) {
            int SFcount = 0;
            //System.out.println(Arrays.toString(raw));
            for (int j = 0; j < count - 3; j++) {
                if (!(straightIndex[j] && flushIndex[j])) continue;
                SFcount = 1;
                int index = raw[j].val;
                SFindex[j] = true;
                for (int i = j; i < count; i++) {
                    if (straightIndex[i] && flushIndex[i]) {
                        if (raw[i].val == index - 1) {
                            SFcount++;
                            index--;
                            SFindex[i] = true;
                        }
                    }
                    if (index == 2 && SFcount >= 4) {
                        for (int k = 0; k < count; k++) {
                            if (raw[k].val == 14 && straightIndex[k] && flushIndex[k]) {
                                SFcount++;
                                SFindex[k] = true;
                            }
                        }
                    }
                }
                //System.out.println(SFcount);
                if (SFcount >= 5) {
                    strength = 8;
                }
                if (strength != 8) SFindex = new boolean[count];
            }
        }

        LinkedList<Card> rawList = new LinkedList<>(Arrays.asList(raw));
        Card[] toReturn = new Card[7];
        /*System.out.println(Arrays.toString(raw));
        System.out.println(Arrays.toString(straightIndex));
        System.out.println(Arrays.toString(flushIndex));
        System.out.println(strength);*/
        /* COMPUTE FINAL BEST HAND */
        switch (strength) {
            case 0:
                toReturn = raw;
                break;
            case 1:
                for (int i = 0; i < count; i++) {
                    if (matches[i] == 2) {
                        rawList.addFirst(rawList.remove(i));
                    }
                }
                toReturn = rawList.toArray(new Card[]{});
                break;
            case 2:
                int highestPair = 0;
                int otherPair = 0;
                for (int i = 0; i < count; i++) {
                    if (matches[i] == 2) {
                        if (highestPair < raw[i].val) {
                            highestPair = raw[i].val;
                        }
                        if (raw[i].val < highestPair && otherPair < raw[i].val) {
                            otherPair = raw[i].val;
                        }
                    }
                }
                for (int i = 0; i < count; i++) {
                    if (rawList.get(i).val == highestPair) {
                        rawList.add(0, rawList.remove(i));
                    } else if (rawList.get(i).val == otherPair) {
                        rawList.add(2, rawList.remove(i));
                    }
                }
                ;
                toReturn = rawList.toArray(new Card[]{});
                break;
            case 3:
                int highestTrips2 = 0;
                for (int i = 0; i < count; i++) {
                    if (matches[i] == 3 && highestTrips2 < raw[i].val) {
                        highestTrips2 = raw[i].val;
                    }
                }
                for (int i = 0; i < count; i++) {
                    if (rawList.get(i).val == highestTrips2) {
                        rawList.addFirst(rawList.remove(i));
                    }
                }
                toReturn = rawList.toArray(new Card[]{});
                break;
            case 4:
                LinkedList<Card> straightList = new LinkedList<>();
                int index = 0;


                boolean hasWheel = false;
                int highestStr = 0;
                int secondHighestStr = 0;
                for (int i = 0; i < count; i++) {
                    if (straightIndex[i])
                    if (raw[i].val > highestStr) highestStr = raw[i].val;
                }
                for (int i = 0; i < count; i++) {
                    if (straightIndex[i])
                    if (secondHighestStr < raw[i].val && raw[i].val != highestStr) secondHighestStr = raw[i].val;
                }
                if (highestStr == 14 && secondHighestStr == 5) hasWheel = true;

                if (!hasWheel) {
                    if (highestStr == 14 && secondHighestStr == 13) index = 15;
                    else if (highestStr == 14) {
                        index = secondHighestStr + 1;
                    }
                    for (int i = 0; i < rawList.size(); i++) {
                        if (straightIndex[i]) {
                            if (index == 0) {
                                index = raw[i].val;
                                straightList.add(raw[i]);
                            } else if (raw[i].val == index - 1) {
                                straightList.add(raw[i]);
                                index--;
                            }
                        }
                    }
                } else {
                    index = 5;
                    for (int i = 0; i < count; i++) {
                        if (straightIndex[i] && raw[i].val == index) {
                            straightList.add(raw[i]);
                            index--;
                            if (index == 1) index = 14;
                            i = -1;
                        }
                    }
                }
                toReturn = straightList.toArray(new Card[]{});
                break;
            case 8:
            case 9:
                LinkedList<Card> straightFlushList = new LinkedList<>();
                int index2 = 0;


                boolean hasWheel2 = false;
                int highestStr2 = 0;
                int secondHighestStr2 = 0;
                for (int i = 0; i < count; i++) {
                    if (SFindex[i])
                        if (raw[i].val > highestStr2) highestStr2 = raw[i].val;
                }
                for (int i = 0; i < count; i++) {
                    if (SFindex[i])
                        if (secondHighestStr2 < raw[i].val && raw[i].val != highestStr2) secondHighestStr2 = raw[i].val;
                }
                if (highestStr2 == 14 && secondHighestStr2 == 5) hasWheel2 = true;

                if (!hasWheel2) {
                    if (highestStr2 == 14 && secondHighestStr2 == 13) index2 = 15;
                    else if (highestStr2 == 14) {
                        index2 = secondHighestStr2 + 1;
                    }
                    for (int i = 0; i < rawList.size(); i++) {
                        if (SFindex[i]) {
                            if (index2 == 0) {
                                index2 = raw[i].val;
                                straightFlushList.add(raw[i]);
                            } else if (raw[i].val == index2 - 1) {
                                straightFlushList.add(raw[i]);
                                index2--;
                            }
                        }
                    }
                } else {
                    index2 = 5;
                    for (int i = 0; i < count; i++) {
                        if (SFindex[i] && raw[i].val == index2) {
                            straightFlushList.add(raw[i]);
                            index2--;
                            if (index2 == 1) index2 = 14;
                            i = -1;
                        }
                    }
                }
                toReturn = straightFlushList.toArray(new Card[]{});
                break;
            case 5:
                LinkedList<Card> flushList = new LinkedList<>();
                for (int i = 0; i < count; i++) {
                    if (flushIndex[i]) {
                        boolean added = false;
                        if (flushList.size() == 0) flushList.add(raw[i]);
                        else {
                            for (int j = 0; j < flushList.size(); j++) {
                                if (raw[i].val > flushList.get(j).val) {
                                    flushList.add(j, raw[i]);
                                    added = true;
                                    break;
                                }
                            }
                            if (!added) flushList.add(raw[i]);
                        }
                    }
                }
                toReturn = flushList.toArray(new Card[]{});
                break;
            case 6:
                int highestTrips = 0;
                int highestPair2 = 0;
                for (int i = 0; i < count; i++) {
                    if (matches[i] == 3 && highestTrips < raw[i].val) {
                        highestTrips = raw[i].val;
                    }
                }
                for (int i = 0; i < count; i++) {
                    if (matches[i] >= 2 && highestPair2 < raw[i].val && highestTrips != raw[i].val) {
                        highestPair2 = raw[i].val;
                    }
                }
                for (int i = 0; i < count; i++) {
                    int j = rawList.get(i).val;
                    if (j == highestTrips) {
                        rawList.addFirst(rawList.remove(i));
                    }
                }
                for (int i = 0; i < count; i++) {
                    int j = rawList.get(i).val;
                    if (j == highestPair2) {
                        rawList.add(3, rawList.remove(i));
                    }
                }
                toReturn = rawList.toArray(new Card[]{});
                break;
            case 7:
                for (int i = 0; i < count; i++) {
                    if (matches[i] == 4) {
                        rawList.addFirst(rawList.remove(i));
                    }
                }
                toReturn = rawList.toArray(new Card[]{});
                break;
        }


        return Arrays.copyOf(toReturn, 5);
    }

    public Card[] bestHand(Card[] board) {
        if (board[0] == null) {
            return Arrays.copyOf(holeCards, 2);
        } else {
            //hand rankings
            int count = 0;
            for (Card c : board) {
                if (c != null)
                    count++;
            }
            Card[] raw = new Card[count];
            for (int i = 0; i < count - (0); i++) {
                if (board[i] != null)
                    raw[i + (0)] = board[i];
            }
            Arrays.sort(raw);

            return bestHandHelper(raw, count);

        }
    } // bestHand

    // Overloaded method that accepts holeCards
    public Card[] bestHand(Card[] board, Card[] difHoleCards) {
        if (board[0] == null) {
            return Arrays.copyOf(difHoleCards, 2);
        } else {
            //hand rankings
            int count = 2;
            for (Card c : board) {
                if (c != null)
                    count++;
            }
            Card[] raw = new Card[count];
            raw[0] = difHoleCards[0];
            raw[1] = difHoleCards[1];
            for (int i = 0; i < count - 2; i++) {
                if (board[i] != null)
                    raw[i + 2] = board[i];
            }
            Arrays.sort(raw);

            return bestHandHelper(raw, count);


        }
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
        return getTotalBet();
    }

    public int getActualBet() {
        return getTotalBet() - getFrontMoney();
    }

    public int[] getEquity(int numPlayers, Card[] board2, int smps) {
        return getEquity(holeCards, numPlayers, board2, smps);
    }
    //returns [countTheyWin,countWeChop,countIWin]
    public int[] getEquity(Card[] cards, int numPlayers, Card[] board2, int smps) {
        Card[] board = new Card[5];
        for (int i = 0; i < 5; i++) {
            if (board2[i] == null) board[i] = null;
            else board[i] = board2[i].copyOf();
        }

        //first row is board, second is me, the rest are other players
        Card[][] eq = new Card[1 + numPlayers][];
        for (int i = 1; i < 1 + numPlayers; i++) {
            eq[i] = new Card[2];
        }
        eq[0] = board;
        eq[1][0] = cards[0];
        eq[1][1] = cards[1];

        int count = 0;
        for (Card c : board) if (c != null) count++;
        Card[] blacklist = new Card[2 + count];
        for (int i = 0; i < 2 + count; i++) {
            if (i < 2) blacklist[i] = cards[i];
            else if (board[i - 2] != null) blacklist[i] = board[i - 2];
        }
        Deck deck = new Deck(blacklist);
        int[] counts = new int[3];
        eqIterate(counts, eq, deck, smps);

        return counts;
    }

    private void eqIterate(int[] counts, Card[][] eq2, Deck deck2, int smps) {
        final boolean debug = false;
        final long samples = smps;

        Card[][] eq = copyEq(eq2);
        Deck deck = deck2.deepCopy();
        int boardSize = 0;
        for (Card c : eq[0]) if (c != null) boardSize++;

        if (debug) System.out.println("un shuffled deck: " + deck.mainDeck.toString());
        int deckSize = deck.mainDeck.size();
        for (int k = 0; k < samples; k++) {
            for (int i = 0; i < deckSize * 4; i++) {
                int j = (int) (Math.random() * deckSize);
                deck.mainDeck.add(deck.mainDeck.remove(j));
            }
            int size = (5 - boardSize) + (2 * (eq2.length - 2));
            if (debug) System.out.println("shuffled deck: " + deck.mainDeck.toString());
            LinkedList<Card> boardAndHand = deck.deepCopy().mainDeck;
            int index = 0;
            for (int j = 0; j < (deckSize - size); j += size) {
                for (int i = 2; i < eq.length; i++) {
                    eq[i][0] = boardAndHand.get(index).copyOf();
                    index++;
                    eq[i][1] = boardAndHand.get(index).copyOf();
                    index++;
                }
                for (int i = boardSize; i < 5; i++) {
                    eq[0][i] = boardAndHand.get(index).copyOf();
                    index++;
                }
                int outcome = 1;
                Card[] bh1 = bestHand(eq[0], eq[1]);
                for (int i = 2; i < eq.length; i++) {
                    Card[] bh2 = bestHand(eq[0], eq[i]);
                    if (parent.decideWinner(bh1, bh2) < 0) {
                        outcome = -1;
                        break;
                    }
                    if (parent.decideWinner(bh1, bh2) == 0) {
                        if (outcome != -1) outcome = 0;
                    }
                }
                counts[outcome + 1]++;
                if (debug) {
                    System.out.println(Arrays.toString(eq[1]) + Arrays.toString(eq[2]) + Arrays.toString(eq[0]) + outcome);
                    System.out.println("str " + Arrays.toString(strength(bh1)) + Arrays.toString(bh1));
                    Card[] bhb = bestHand(eq[0]);
                    System.out.println("strb " + Arrays.toString(strength(bhb)) + Arrays.toString(bhb));
                    Card[] bh2 = bestHand(eq[0], eq[2]);
                    System.out.println("str2 " + Arrays.toString(strength(bh2)) + Arrays.toString(bh2));
                }
            }
        }
    }

    private Card[][] copyEq(Card[][] eq) {
        Card[][] newEq = new Card[eq.length][];
        for (int i = 0; i < eq.length; i++) {
            newEq[i] = new Card[eq[i].length];
            for (int j = 0; j < eq[i].length; j++) {
                if (eq[i][j] == null) newEq[i][j] = null;
                else newEq[i][j] = eq[i][j].copyOf();
            }
        }
        return newEq;
    }

    public int getTotalBet() {
        int bet = 0;
        for (Pot i : parent.pots) {
            bet += i.bet;
        }
        return bet;
    }

    public int getFrontMoney() {
        int globalFrontMoney = 0;
        for (Pot i : parent.pots) {
            globalFrontMoney += i.callersAmt[parent.players.indexOf(this)];
        }
        return globalFrontMoney;
    }

    public double getPotOdds() {
        return (double) getActualBet() / (double) (getPot() + getActualBet());
    }

    public double getPotOdds(double actualBet, double actualPot) {
        return (double) actualBet / (double) (actualPot + actualBet);
    }

    public double idealBet(double equity, double actualBet, double actualPot) {
        double b = actualBet;
        double p = actualPot;
        double y = equity;
        double a = (double) parent.activePlayers().size() - 1.0;
        double numerator = -((b+p) * y - b);
        double denominator = (a * y) - 1;
        return (numerator / denominator);
    }

    public int getPot() {
        int pot = 0;
        for (Pot i : parent.pots) {
            pot += i.potAmt;
        }
        return pot;
    }


    // input: what i would be willing to call total
    // output: call or fold depending on value
    int sanitizeBetV(int tr) {
        int toReturn = tr;
        if (getBet() * 1.5 > toReturn) return -1;
        if (getBet() > toReturn) return 0;
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

    // input: how much i want to raise by
    // output: input obeying raising rules
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
