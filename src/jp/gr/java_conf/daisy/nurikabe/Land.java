package jp.gr.java_conf.daisy.nurikabe;
import java.util.HashSet;
import java.util.Set;

public class Land {
	private Set<Point> points;
	public Land(Point center) {
		points = new HashSet<Point>();
		points.add(center);
	}
	
	public void add(Point p) {
		points.add(p);
	}
	
	public int size() {
		return points.size();
	}
	
	public Set<Point> getPoints() {
		return points;
	}
}
