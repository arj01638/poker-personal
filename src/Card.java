
public class Card implements Comparable<Card> {

	int val;
	int suit;
	
	Card(int value, int suit) {
		this.val = value;
		this.suit = suit;
	} // Card
	
	@Override
	public String toString() {
		return val + "." + suit;
	}

	@Override
	public int compareTo(Card o) {
		return o.val - this.val;
	}
	
	public int compareToS(Card o) {
		if (o.suit != this.suit) return 1;
		return 0;
	}
	
}
