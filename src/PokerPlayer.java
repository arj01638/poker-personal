import java.util.Arrays;
import java.util.LinkedList;

public abstract class PokerPlayer {

    int BB = 500;
    int SB = BB / 2;
    int STARTING_BANK = 10000;
    public int bank;
    public final Card[] holeCards = new Card[2];
    public boolean inTheHand = true;
    public boolean allIn = false;
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

    // Used to use permutations, but manually checking a few metrics
    // and assembling the best hand improves performance >threefold.
    public Card[] bestHand(Card[] board, boolean useHoleCards) {
        Card[] toReturn = new Card[7];
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
            Arrays.sort(raw);


            /* COMPUTE METRICS */
            int[] matches = new int[count];
            int[] suits = new int[4];
            boolean hasStraight = false;
            boolean hasFlush = false;
            //int flushSuit = -1;
            boolean[] straightIndex = new boolean[count];
            boolean[] flushIndex = new boolean[count];
            boolean[] wheelCrt = new boolean[5];
            boolean wheel;
            for (int i = 0; i < count; i++) {
                //suit tallying
                suits[raw[i].suit - 1]++;
                if (suits[raw[i].suit - 1] >= 5) {
                    //flushSuit = raw[i].suit;
                    hasFlush = true;
                    for (int j = 0; j < count; j++) {
                        if (raw[j].suit == raw[i].suit) {
                            flushIndex[j] = true;
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
                if (i + 4 < count) {
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
                    }
                    if (straightCount >= 5) {
                        hasStraight = true;
                    }
                    if (!hasStraight) {
                        straightIndex = new boolean[count];
                    }
                }

                if (raw[i].val == 14)
                    wheelCrt[4] = true;
                if (raw[i].val == 5)
                    wheelCrt[0] = true;
                if (raw[i].val == 4)
                    wheelCrt[1] = true;
                if (raw[i].val == 3)
                    wheelCrt[2] = true;
                if (raw[i].val == 2)
                    wheelCrt[3] = true;
            }
            wheel = true;
            for (int i = 0; i < 5; i++) {
                if (!wheelCrt[i]) {
                    wheel = false;
                    break;
                }
            }
            if (wheel) {
                hasStraight = true;
                for (int i = 0; i < count; i++) {
                    if (raw[i].val < 6 || raw[i].val == 14)
                        straightIndex[i] = true;
                }
            }


            /* COMPUTE BEST STRENGTH */
            int strength = 0;

            int numFours = 0;
            int numThrees = 0;
            int numTwos = 0;
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
            if (numThrees > 0 && numTwos > 1) strength = 6;
            //quads
            if (numFours > 0) strength = 7;
            //straight flush
            if (hasStraight) {
                int SFcount = 0;
                int index = 0;
                for (int j = 0; j < count - 4; j++) {
                    for (int i = j; i < count; i++) {
                        if (straightIndex[i] && flushIndex[i]) {
                            if (index == 0) {
                                index = raw[i].val;
                                SFcount++;
                            } else if (raw[i].val == index - 1) {
                                SFcount++;
                                index--;
                            }
                        }
                    }
                }
                if (SFcount >= 5) {
                    strength = 8;
                    if (raw[0].val == 14 && straightIndex[0] && flushIndex[0]
                            || raw[1].val == 14 && straightIndex[1] && flushIndex[1]
                            || raw[2].val == 14 && straightIndex[2] && flushIndex[2])
                        strength = 9;
                }
                if (wheel) {
                    SFcount = 0;
                    for (int i = 0; i < count; i++) {
                        if (raw[i].val < 6 || raw[i].val == 14)
                            if (straightIndex[i] && flushIndex[i])
                                SFcount++;
                    }

                    if (SFcount >= 5) {
                        strength = 8;
                    }
                }
            }

            LinkedList<Card> rawList = new LinkedList<>(Arrays.asList(raw));
            /* COMPUTE FINAL BEST HAND */
            switch (strength) {
                case 0:
                    toReturn = raw;
                    break;
                case 1:
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 2) {
                            Card x = rawList.get(i);
                            rawList.remove(i);
                            rawList.addFirst(x);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
                case 2:
                    int highestPair = 0;
                    int highestPairIndex = -1;
                    int otherPair = 0;
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 2) {
                            if (highestPair < raw[i].val) {
                                otherPair = highestPair;
                                highestPair = raw[i].val;
                                highestPairIndex = i;
                                i = 0;
                            }
                        }
                    }
                    Card x = rawList.get(highestPairIndex);
                    rawList.remove(highestPairIndex);
                    rawList.addFirst(x);
                    boolean foundHighestPair = false;
                    for (int i = 1; i < count; i++) {
                        if (rawList.get(i).val == highestPair) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.add(1, y);
                            i = 2;
                            foundHighestPair = true;
                        } else if (foundHighestPair && rawList.get(i).val == otherPair) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.add(2, y);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
                case 3:
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 3) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.addFirst(y);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
                case 4:
                    LinkedList<Card> straightList = new LinkedList<>();
                    int index = 0;
                    if (!wheel) {
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
                        for (int i = 0; i < count; i++) {
                            if (straightIndex[i] && raw[i].val != 14) {
                                if (index == 0) {
                                    index = raw[i].val;
                                    straightList.add(raw[i]);
                                } else if (raw[i].val == index - 1) {
                                    straightList.add(raw[i]);
                                    index--;
                                }
                            }
                        }
                        if (straightList.size() != 5) {
                            for (int i = 0; i < count; i++) {
                                if (straightIndex[i] && raw[i].val == 14) {
                                    straightList.add(raw[i]);
                                    break;
                                }
                            }
                        }
                    }
                    toReturn = straightList.toArray(new Card[]{});
                    break;
                case 8:
                case 9:
                    LinkedList<Card> straightFlushList = new LinkedList<>();
                    int index2 = 0;
                    if (!wheel) {
                        for (int i = 0; i < rawList.size(); i++) {
                            if (straightIndex[i] && flushIndex[i]) {
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
                        for (int i = 0; i < count; i++) {
                            if (straightIndex[i] && flushIndex[i] && raw[i].val != 14) {
                                if (index2 == 0) {
                                    index2 = raw[i].val;
                                    straightFlushList.add(raw[i]);
                                } else if (raw[i].val == index2 - 1) {
                                    straightFlushList.add(raw[i]);
                                    index2--;
                                }
                            }
                        }
                        if (straightFlushList.size() != 5) {
                            for (int i = 0; i < count; i++) {
                                if (straightIndex[i] && raw[i].val == 14) {
                                    straightFlushList.add(raw[i]);
                                    break;
                                }
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
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 3) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.addFirst(y);
                        }
                    }
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 2) {
                            Card z = raw[i];
                            rawList.remove(z);
                            rawList.add(3, z);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
                case 7:
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 4) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.addFirst(y);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
            }

        }
        return Arrays.copyOf(toReturn, 5);
    } // bestHand

    // Overloaded method that accepts custom holeCards
    public Card[] bestHand(Card[] board, Card[] difHoleCards) {
        Card[] toReturn = new Card[7];
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


            /* COMPUTE METRICS */
            int[] matches = new int[count];
            int[] suits = new int[4];
            boolean hasStraight = false;
            boolean hasFlush = false;
            //int flushSuit = -1;
            boolean[] straightIndex = new boolean[count];
            boolean[] flushIndex = new boolean[count];
            boolean[] wheelCrt = new boolean[5];
            boolean wheel;
            for (int i = 0; i < count; i++) {
                //suit tallying
                suits[raw[i].suit - 1]++;
                if (suits[raw[i].suit - 1] >= 5) {
                    //flushSuit = raw[i].suit;
                    hasFlush = true;
                    for (int j = 0; j < count; j++) {
                        if (raw[j].suit == raw[i].suit) {
                            flushIndex[j] = true;
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
                if (i + 4 < count) {
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
                    }
                    if (straightCount >= 5) {
                        hasStraight = true;
                    }
                    if (!hasStraight) {
                        straightIndex = new boolean[count];
                    }
                }

                if (raw[i].val == 14)
                    wheelCrt[4] = true;
                if (raw[i].val == 5)
                    wheelCrt[0] = true;
                if (raw[i].val == 4)
                    wheelCrt[1] = true;
                if (raw[i].val == 3)
                    wheelCrt[2] = true;
                if (raw[i].val == 2)
                    wheelCrt[3] = true;
            }
            wheel = true;
            for (int i = 0; i < 5; i++) {
                if (!wheelCrt[i]) {
                    wheel = false;
                    break;
                }
            }
            if (wheel) {
                hasStraight = true;
                for (int i = 0; i < count; i++) {
                    if (raw[i].val < 6 || raw[i].val == 14)
                        straightIndex[i] = true;
                }
            }


            /* COMPUTE BEST STRENGTH */
            int strength = 0;

            int numFours = 0;
            int numThrees = 0;
            int numTwos = 0;
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
            if (numThrees > 0 && numTwos > 1) strength = 6;
            //quads
            if (numFours > 0) strength = 7;
            //straight flush
            if (hasStraight) {
                int SFcount = 0;
                int index = 0;
                for (int j = 0; j < count - 4; j++) {
                    for (int i = j; i < count; i++) {
                        if (straightIndex[i] && flushIndex[i]) {
                            if (index == 0) {
                                index = raw[i].val;
                                SFcount++;
                            } else if (raw[i].val == index - 1) {
                                SFcount++;
                                index--;
                            }
                        }
                    }
                }
                if (SFcount >= 5) {
                    strength = 8;
                    if (raw[0].val == 14 && straightIndex[0] && flushIndex[0]
                            || raw[1].val == 14 && straightIndex[1] && flushIndex[1]
                            || raw[2].val == 14 && straightIndex[2] && flushIndex[2])
                        strength = 9;
                }
                if (wheel) {
                    SFcount = 0;
                    for (int i = 0; i < count; i++) {
                        if (raw[i].val < 6 || raw[i].val == 14)
                            if (straightIndex[i] && flushIndex[i])
                                SFcount++;
                    }

                    if (SFcount >= 5) {
                        strength = 8;
                    }
                }
            }

            LinkedList<Card> rawList = new LinkedList<>(Arrays.asList(raw));
            /* COMPUTE FINAL BEST HAND */
            switch (strength) {
                case 0:
                    toReturn = raw;
                    break;
                case 1:
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 2) {
                            Card x = rawList.get(i);
                            rawList.remove(i);
                            rawList.addFirst(x);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
                case 2:
                    int highestPair = 0;
                    int highestPairIndex = -1;
                    int otherPair = 0;
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 2) {
                            if (highestPair < raw[i].val) {
                                otherPair = highestPair;
                                highestPair = raw[i].val;
                                highestPairIndex = i;
                                i = 0;
                            }
                        }
                    }
                    Card x = rawList.get(highestPairIndex);
                    rawList.remove(highestPairIndex);
                    rawList.addFirst(x);
                    boolean foundHighestPair = false;
                    for (int i = 1; i < count; i++) {
                        if (rawList.get(i).val == highestPair) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.add(1, y);
                            i = 2;
                            foundHighestPair = true;
                        } else if (foundHighestPair && rawList.get(i).val == otherPair) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.add(2, y);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
                case 3:
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 3) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.addFirst(y);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
                case 4:
                    LinkedList<Card> straightList = new LinkedList<>();
                    int index = 0;
                    if (!wheel) {
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
                        for (int i = 0; i < count; i++) {
                            if (straightIndex[i] && raw[i].val != 14) {
                                if (index == 0) {
                                    index = raw[i].val;
                                    straightList.add(raw[i]);
                                } else if (raw[i].val == index - 1) {
                                    straightList.add(raw[i]);
                                    index--;
                                }
                            }
                        }
                        if (straightList.size() != 5) {
                            for (int i = 0; i < count; i++) {
                                if (straightIndex[i] && raw[i].val == 14) {
                                    straightList.add(raw[i]);
                                    break;
                                }
                            }
                        }
                    }
                    toReturn = straightList.toArray(new Card[]{});
                    break;
                case 8:
                case 9:
                    LinkedList<Card> straightFlushList = new LinkedList<>();
                    int index2 = 0;
                    if (!wheel) {
                        for (int i = 0; i < rawList.size(); i++) {
                            if (straightIndex[i] && flushIndex[i]) {
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
                        for (int i = 0; i < count; i++) {
                            if (straightIndex[i] && flushIndex[i] && raw[i].val != 14) {
                                if (index2 == 0) {
                                    index2 = raw[i].val;
                                    straightFlushList.add(raw[i]);
                                } else if (raw[i].val == index2 - 1) {
                                    straightFlushList.add(raw[i]);
                                    index2--;
                                }
                            }
                        }
                        if (straightFlushList.size() != 5) {
                            for (int i = 0; i < count; i++) {
                                if (straightIndex[i] && raw[i].val == 14) {
                                    straightFlushList.add(raw[i]);
                                    break;
                                }
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
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 3) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.addFirst(y);
                        }
                    }
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 2) {
                            Card z = raw[i];
                            rawList.remove(z);
                            rawList.add(3, z);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
                case 7:
                    for (int i = 0; i < count; i++) {
                        if (matches[i] == 4) {
                            Card y = rawList.get(i);
                            rawList.remove(i);
                            rawList.addFirst(y);
                        }
                    }
                    toReturn = rawList.toArray(new Card[]{});
                    break;
            }

        }
        return Arrays.copyOf(toReturn, 5);
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
        return getTotalBet() - getFrontMoney();
    }

    //returns [countIWin,countTheyWin,countWeChop]
    public int[] getEquity(int numPlayers, Card[] board2) {
        Card[] board = new Card[5];
        for (int i = 0; i < 5; i++) {
            if (board2[i] == null) board[i] = null;
            else board[i] = board2[i].copyOf();
        }

        //first row is board, second is me, the rest are other players
        Card[][] eq = new Card[1+numPlayers][];
        for (int i = 1; i < 1+numPlayers; i++) {
            eq[i] = new Card[2];
        }
        eq[0] = board;
        eq[1][0] = holeCards[0];
        eq[1][1] = holeCards[1];

        int count = 0;
        for (Card c : board) if (c != null) count++;
        Card[] blacklist = new Card[2 + count];
        for (int i = 0; i < 2 + count; i++) {
            if (i < 2) blacklist[i] = holeCards[i];
            else if (board[i - 2] != null) blacklist[i] = board[i - 2];
        }
        Deck deck = new Deck(blacklist);
        int[] counts = new int[3];
        eqIterate(counts, eq, deck);


        return counts;
    }

    //int skipper = 0;
    private int[] eqIterate(int[] counts, Card[][] eq2, Deck deck2) {
        //skipper++;
        //if (skipper > 1000 && !(skipper % 2 == 0)) return counts;
        Card[][] eq = copyEq(eq2);
        Deck deck = deck2.deepCopy();
        boolean stopIterating = false;

        //if any player cards are null
        for (int i = 0; i < eq.length; i++) {
            if (i > 0) {
                for (int j = 0; j < eq[i].length; j++) {
                    if (eq[i][j] == null) {
                        stopIterating = true;
                        while ((eq[i][j] = deck.randomCard()) != null) {
                            eqIterate(counts, eq, deck2.deepCopy().remove(eq[i][j]));
                        }
                        deck = deck2.deepCopy();
                    }
                }
            }
        }

        if (!stopIterating) {
            //if any board cards are null
            for (int i = 0; i < eq[0].length; i++) {
                if (eq[0][i] == null) {
                    while ((eq[0][i] = deck.randomCard()) != null) {
                        eqIterate(counts, eq, deck2.deepCopy().remove(eq[0][i]));
                    }
                    stopIterating = true;
                }
            }
        }

        //SHOWDOWN
        if (!stopIterating) {
            Card[] myBH = bestHand(eq[0], eq[1]);
            boolean win = true;
            for (int i = 2; i < eq.length; i++) {
                Card[] villainBH = bestHand(eq[0], eq[i]);
                if (parent.decideWinner(myBH, villainBH) < 0) {
                    counts[1]++;
                    System.out.println(Arrays.toString(eq[1]) + Arrays.toString(eq[2]) + Arrays.toString(eq[0]) + "loss");
                    return counts;
                }
                if (parent.decideWinner(myBH, villainBH) == 0) {
                    win = false;
                }
            }
            if (win) counts[0]++; else counts[2]++;

            if (win)
                System.out.println(Arrays.toString(eq[1]) + Arrays.toString(eq[2]) + Arrays.toString(eq[0]) + "win");
            else
                System.out.println(Arrays.toString(eq[1]) + Arrays.toString(eq[2]) + Arrays.toString(eq[0]) + "draw");
        }

        return counts;
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

    public int getPotOdds() {
        return getBet() / (getPot() + getBet());
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
