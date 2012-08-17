package jp.gr.java_conf.daisy.nurikabe;
import java.util.HashSet;
import java.util.Set;

public class Land {
	private final Point center;
	private Set<Point> points;
	
	public Land(Point center) {
		this.center = center;
		points = new HashSet<Point>();
		points.add(center);
	}
	
	public Point getCenter() {
		return center;
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
	
	@Override
	public String toString() {
		return "land" + center + points;
	}
}
