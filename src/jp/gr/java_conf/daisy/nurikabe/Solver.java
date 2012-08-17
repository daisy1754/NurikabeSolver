package jp.gr.java_conf.daisy.nurikabe;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class Solver {
	private int[][] stage;
	private final Point stageSize;
	private static final int TYPE_UNDEFINED = 0;
	private static final int TYPE_LAND = -1; // belong to island.
	private static final int TYPE_BLOCK = -2;
	private static final int TYPE_LAND_ISOLATED = -3;
	private static final int TYPE_OUT_OF_BOUNDS = -10;
	private final Map<Point, Integer> numbersMap;
	private Set<Point> blocks;
	private Map<Point, Land> unresolvedLands;
	private Map<Point, Point> landToCenter;
	private Set<Point> isolatedLand;
	
	public Solver(List<String> inputs) {
		numbersMap = new HashMap<Point, Integer>();
		unresolvedLands = new HashMap<Point, Land>();
		landToCenter = new HashMap<Point, Point>();
		isolatedLand = new HashSet<Point>();
		blocks = new HashSet<Point>();
		stageSize = new Point(inputs.get(0).split(" ").length, inputs.size());
		stage = new int[stageSize.x][stageSize.y];
		for (int y = 0; y < inputs.size(); y++) {
			String line = inputs.get(y);
			String[] tokens = line.split(" ");
			for (int x = 0; x < tokens.length; x++) {
				if (tokens[x].charAt(0) == '_') {
					stage[x][y] = TYPE_UNDEFINED;
				} else {
					stage[x][y] = Integer.parseInt(tokens[x]) - 0;
					Point point = new Point(x, y);
					numbersMap.put(point, stage[x][y]);
					landToCenter.put(point, point);
					unresolvedLands.put(point, new Land(point));
				}
			}
		}
	}

	public boolean solve() {
		for (Point p: numbersMap.keySet()) {
			// consume 1
			if (numbersMap.get(p) == 1) {
				tryToPaint(p.x, p.y + 1);
				tryToPaint(p.x, p.y - 1);
				tryToPaint(p.x + 1, p.y);
				tryToPaint(p.x - 1, p.y);
				unresolvedLands.remove(p);
			} else {	
				// paint one-width space like '3_1'
				if (getTileState(p.x, p.y + 2) > 0) 
					tryToPaint(p.x, p.y + 1);
				if (getTileState(p.x + 2, p.y) > 0) 
					tryToPaint(p.x + 1, p.y);
			
				// care for naname
				if (getTileState(p.x + 1, p.y + 1) > 0) {
					tryToPaint(p.x, p.y + 1);
					tryToPaint(p.x + 1, p.y);
				}
				if (getTileState(p.x - 1, p.y + 1) > 0) {
					tryToPaint(p.x, p.y + 1);
					tryToPaint(p.x - 1, p.y);
				}
			}				
		}
		
		while (true) {
			boolean cannotPaint = true;
			if (checkAroundBlocks() > 0)
				cannotPaint = false;
			if (checkAroundLand() > 0)
				cannotPaint = false;
			if (cannotPaint) {
				int numberOfFound = checkForIsolatedLand();
				if (numberOfFound > 0)
					cannotPaint = false;
			}
			if (isolatedLand.size() > 0) {
				mergeIsolatedLandToLand();
			}
			for (Land land : unresolvedLands.values()) {
				if (land.size() == getTileState(land.getCenter()) - 1) {
				List<Point> canBeLand = getPaintableTilesAround(land, 2);
					if (canBeLand != null) {
						Point p1 = canBeLand.get(0);
						Point p2 = canBeLand.get(1);
						if (Math.abs(p1.x - p2.x) == 1 && Math.abs(p1.y - p2.y) == 1) {
							if (getTileState(p1.x, p2.y) == TYPE_UNDEFINED)
								tryToPaint(p1.x, p2.y);
							if (getTileState(p2.x, p1.y) == TYPE_UNDEFINED)
								tryToPaint(p2.x, p1.y);
						}
					}
				}
			}
			if (cannotPaint)
				break;
		}
		
		return getUndefinedTiles().size() == 0;
	}
	
	private int checkAroundLand() {
		int count = 0;
		Map<Point, Land> copyOfUnresolvedLands 
			= new HashMap<Point, Land>(unresolvedLands);
		for (Point center: copyOfUnresolvedLands.keySet()) {
			Land land = copyOfUnresolvedLands.get(center);
			int maxSize = getTileState(center);
			Point paintablePoint;
			while (land.size() < maxSize 
					&& (paintablePoint = getPaintableTileAround(land)) != null) {
				count++;
				land.add(paintablePoint);
				tryToCheckAsLand(paintablePoint.x, paintablePoint.y, center);
				
				Iterator<Point> iterator = isolatedLand.iterator();
				while (iterator.hasNext()) {
					Point isolated = iterator.next();
					int xDiff = Math.abs(isolated.x - paintablePoint.x);
					int yDiff = Math.abs(isolated.y - paintablePoint.y);
					if (xDiff + yDiff == 1) {
						count++;
						land.add(isolated);
						tryToCheckAsLand(isolated.x, isolated.y, center);
						iterator.remove();
					}
				}
			}
			if (maxSize == land.size()) {
				count++;
				unresolvedLands.remove(center);
				paintAroundLand(land);
			}
		}
		return count;
	}

	private int checkForIsolatedLand() {
		int count = 0;
		for (int y = 0; y < stageSize.y; y++) {
			for (int x = 0; x < stageSize.x; x++) {
				if (stage[x][y] == TYPE_UNDEFINED) {
					if ((getTileState(x, y - 1) == TYPE_BLOCK // left up
							&& getTileState(x - 1, y - 1) == TYPE_BLOCK
							&& getTileState(x - 1, y) == TYPE_BLOCK)
						|| (getTileState(x - 1, y) == TYPE_BLOCK // left down
							&& getTileState(x - 1, y + 1) == TYPE_BLOCK
							&& getTileState(x, y + 1) == TYPE_BLOCK)
						|| (getTileState(x, y + 1) == TYPE_BLOCK // down right
							&& getTileState(x + 1, y + 1) == TYPE_BLOCK
							&& getTileState(x + 1, y) == TYPE_BLOCK)
						|| (getTileState(x + 1, y) == TYPE_BLOCK // right up
							&& getTileState(x + 1, y - 1) == TYPE_BLOCK
							&& getTileState(x, y - 1) == TYPE_BLOCK)) {
						stage[x][y] = TYPE_LAND_ISOLATED;
						isolatedLand.add(new Point(x, y));
						count++;
					}
				}
			}
		}
		return count;
	}
	
	private void mergeIsolatedLandToLand() {
		Iterator<Point> iterator = isolatedLand.iterator();
		while (iterator.hasNext()) {
			Point isolated = iterator.next();
			Set<Point> neibors = getNeibors(isolated);
			for (Point neibor: neibors) {
				if (getTileState(neibor) == TYPE_LAND
						|| getTileState(neibor) > 0) {
					Point center = landToCenter.get(neibor);
					if (center != null) {
						Land land = unresolvedLands.get(center);
						land.add(isolated);
						tryToCheckAsLand(isolated.x, isolated.y, center);
						iterator.remove();
						break;
					}
				}
			}
		}
	}
	
	private boolean tryToPaint(Point point) {
		return tryToPaint(point.x, point.y);
	}
	
	private boolean tryToPaint(int x, int y) {
		if (x < 0 || stageSize.x <= x) 
			return false;
		if (y < 0 || stageSize.y <= y)
			return false;
		stage[x][y] = TYPE_BLOCK;
		blocks.add(new Point(x, y));
		return true;
	}
	
	private boolean tryToCheckAsLand(int x, int y, Point center) {
		if (x < 0 || stageSize.x <= x) 
			return false;
		if (y < 0 || stageSize.y <= y)
			return false;
		stage[x][y] = TYPE_LAND;
		landToCenter.put(new Point(x, y), center);

		tryToPaintBetweenLand(center, x, y + 2, x, y + 1);
		tryToPaintBetweenLand(center, x + 2, y, x + 1, y);
		tryToPaintBetweenLand(center, x + 1, y + 1, x, y + 1);
		tryToPaintBetweenLand(center, x + 1, y + 1, x + 1, y);
		tryToPaintBetweenLand(center, x - 1, y + 1, x, y + 1);
		tryToPaintBetweenLand(center, x - 1, y + 1, x - 1, y);
		return true;
	}
	
	private boolean tryToPaintBetweenLand(
			Point center, int landX, int landY, int targetX, int targetY) {
		int tileState = getTileState(landX, landY);
		if (tileState > 0 || tileState == TYPE_LAND) {
			Point oppCenter = landToCenter.get(new Point(landX, landY));
			if (oppCenter != null && !oppCenter.equals(center)) {
				return tryToPaint(targetX, targetY);
			}
		}
		return false;
	}
	
	private Set<Point> getUndefinedTiles() {
		Set<Point> undefinedTiles = new HashSet<Point>();
		for (int y = 0; y < stageSize.y; y++) {
			for (int x = 0; x < stageSize.x; x++) {
				if (stage[x][y] == TYPE_UNDEFINED)
					undefinedTiles.add(new Point(x, y));
			}
		}
		return undefinedTiles;
	}

	private int getTileState(Point p) {
		return getTileState(p.x, p.y);
	}
	
	private int getTileState(int x, int y) {
		if (x < 0 || stageSize.x <= x) 
			return TYPE_OUT_OF_BOUNDS;
		if (y < 0 || stageSize.y <= y)
			return TYPE_OUT_OF_BOUNDS;
		return stage[x][y];
	}
	
	private Set<Point> getNeibors(Point p) {
		Set<Point> set = new HashSet<Point>();
		int[] deltaX = {0, 0, 1, -1};
		int[] deltaY = {1, -1, 0, 0};
		for (int i = 0; i < 4; i++) {
			int x = p.x + deltaX[i];
			int y = p.y + deltaY[i];

			if (x < 0 || stageSize.x <= x || y < 0 || stageSize.y <= y)
				continue;
			set.add(new Point(x, y));
		}
		return set;
	}
	
	private List<Point> getPaintableTilesAround(Land land, int size) {
		Set<Point> points = land.getPoints();
		Set<Point> canBeLand = new HashSet<Point>();
		for (Point p: points) {
			Set<Point> neibors = getNeibors(p);
			for (Point neibor: neibors) {
				if (getTileState(neibor) == TYPE_UNDEFINED) {
					canBeLand.add(neibor);
					if (canBeLand.size() > size)
						return null;
				}
			}
		}
		if (canBeLand.size() == size) {
			return new ArrayList<Point>(canBeLand);
		} else {
			return null;
		}
	}
	
	private Point getPaintableTileAround(Land land) {
		List<Point> tileList =  getPaintableTilesAround(land, 1);
		return tileList == null ? null : tileList.get(0);
	}
	
	private void paintAroundLand(Land land) {
		Set<Point> points = land.getPoints();
		for (Point p: points) {
			Set<Point> neibors = getNeibors(p);
			for (Point neibor: neibors) {
				if (getTileState(neibor) == TYPE_UNDEFINED) {
					tryToPaint(neibor);
				}
			}
		}
	}
	
	private int checkAroundBlocks() {
		int count = 0;
		int numberOfWalls = 0;
		Set<Point> currentBlocks = new HashSet<Point>(blocks);
		Set<Point> consideredBlocks = new HashSet<Point>();
		for (Point p: currentBlocks) {
			if (consideredBlocks.contains(p))
				continue;
			numberOfWalls++;
			Set<Point> canPutBlocks = new HashSet<Point>();
			Stack<Point> stack = new Stack<Point>();
			stack.add(p);
			while (!stack.isEmpty()) {
				Point point = stack.pop();
				consideredBlocks.add(point);
				Set<Point> neibors = getNeibors(point);
				for (Point neibor: neibors) {
					int neiborState = getTileState(neibor);
					if (neiborState == TYPE_UNDEFINED) {
						canPutBlocks.add(neibor);
					} else if (neiborState == TYPE_BLOCK) {
						if (!consideredBlocks.contains(neibor))
							stack.push(neibor);
					}
				}
			}
			if (canPutBlocks.size() == 1) {
				if (numberOfWalls == 1 
						&& consideredBlocks.size() >= currentBlocks.size())
					continue;
				numberOfWalls--;
				Point onlyPoint = new ArrayList<Point>(canPutBlocks).get(0);
				tryToPaint(onlyPoint);
				count++;
			}
		}
		return count;
	}
	
	public void outputTo(PrintStream stream) {
		StringBuilder builder = new StringBuilder();
		builder.append("   ");
		for (int x = 0; x < stageSize.x; x++) {
			builder.append(x >= 10 ? x : " " + x);
		}
		stream.println(builder);
		for (int y = 0; y < stageSize.y; y++) {
			builder = new StringBuilder();
			builder.append(y >= 10 ? y : " " + y);
			builder.append('|');
			for (int x = 0; x < stageSize.x; x++) {
				switch (stage[x][y]) {
				case TYPE_UNDEFINED:
					builder.append(" _");
					break;
				case TYPE_LAND:
					builder.append(" :");
					break;
				case TYPE_LAND_ISOLATED:
					builder.append(" +");
					break;
				case TYPE_BLOCK:
					builder.append(" x");
					break;
				default:
					if (stage[x][y] >= 10)
						builder.append(stage[x][y]);
					else
						builder.append(" " + stage[x][y]);
					break;
				}
			}
			stream.println(builder.toString());
		}
	}
	
	public boolean checkWithAnswerFile(String pathToAnswerFile) {
		File answerFile = new File(pathToAnswerFile);
		BufferedReader reader;
		int numberOfLines = 0;
		try {
			reader = new BufferedReader(new FileReader(answerFile));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(" ");
				if (tokens.length != stageSize.x) {
					System.err.println("" +
							"size is different at line " + numberOfLines);
					return false;
				}
				for (int i = 0; i < tokens.length; i++) {
					int actual = stage[i][numberOfLines];
					if (tokens[i].charAt(0) == ':' 
							&& actual == TYPE_LAND) {
						// ok
					} else if (tokens[i].charAt(0) == 'x' 
							&& actual == TYPE_BLOCK) {
						// ok
					} else {
						try {
							int number = Integer.parseInt(tokens[i]);
							if (number != actual) {
								System.err.println("invalid format at " + i
										+ ", " + numberOfLines + " actual:" 
										+ actual + " expected:" + tokens[i]);
								return false;
							}
						} catch (NumberFormatException exception) {
							System.err.println("invalid format at " + i
									+ ", " + numberOfLines + " actual: " 
									+ actual + " expected: " + tokens[i]);
							return false;
						}
					}
						
				}
				numberOfLines++;
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (numberOfLines != stageSize.y) {
			System.err.println("Height is different");
			return false;
		}
		System.out.println("ok");
		return true;
	}
}
