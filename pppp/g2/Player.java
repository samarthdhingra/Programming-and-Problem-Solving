package pppp.g0;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	// see details below
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private Point[] random_pos = null;
	private Random gen = new Random();
	
	
	private int ratsCountInit = 0;
	private int ratsCountCurrent = 0;
	
	
	private int numberPasses = 0;
	
	private int count = 0;
	
	
	

	// create move towards specified destination
	private static Move move(Point src, Point dst, boolean play)
	{
		double dx = dst.x - src.x;
		double dy = dst.y - src.y;
		double length = Math.sqrt(dx * dx + dy * dy);
		double limit = play ? 0.1 : 0.5;
		if (length > limit) {
			dx = (dx * limit) / length;
			dy = (dy * limit) / length;
		}
		return new Move(dx, dy, play);
	}
	
	private static Move moveSlowly(Point src, Point dst, boolean play)
	{
		double dx = dst.x - src.x;
		double dy = dst.y - src.y;
		double length = Math.sqrt(dx * dx + dy * dy);
		double limit = play ? 0.1 : 0.5;
		if (length > limit) {
			dx = (dx * limit) / length;
			dy = (dy * limit) / length;
		}
		return new Move(dx, dy, play);
	}
	
	
	private static double getDistance(Point a, Point b)
	{
		double x = a.x-b.x;
		double y = a.y-b.y;
		return Math.sqrt(x * x + y * y);
	}

	// generate point after negating or swapping coordinates
	private static Point point(double x, double y,
	                           boolean neg_y, boolean swap_xy)
	{
		if (neg_y) y = -y;
		return swap_xy ? new Point(y, x) : new Point(x, y);
	}

	// specify location that the player will alternate between
	// Init for semi circle sweeping .. 
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats)
	{
		//Initialize total Rats
		this.ratsCountInit = rats.length;
		this.ratsCountCurrent = rats.length;
		
		this.id = id;
		this.side = side;
		int n_pipers = pipers[id].length;
		pos = new Point [n_pipers][6];
		random_pos = new Point [n_pipers];
		pos_index = new int [n_pipers];
		for (int p = 0 ; p != n_pipers ; ++p) {
			// spread out at the door level
			double door = 0.0;
			//if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			if (n_pipers != 1) door = -0.95;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			// first and third position is at the door
			pos[p][0] = pos[p][3] = point(door, side * 0.5, neg_y, swap);
			// Set the second position 
			double xCoordinate = 0.0;
			double yCoordinate = 0.0;
			
			if (p == 3)
			{
				xCoordinate = 0.45*side;
				yCoordinate = 0.35*side;
			}
			else
			{
				xCoordinate = (p * 0.4 / (n_pipers - 1) - 0.2) * side;
				yCoordinate = -0.1*side;
			}	
			pos[p][1] = point(xCoordinate, yCoordinate, neg_y, swap);
			pos[p][2] = point(0, 0.40*side, neg_y, swap);
			
			// second position is chosen randomly in the rat moving area
//			pos[p][1] = null;
			// fourth and fifth positions are outside the rat moving area
			pos[p][4] = point(door * -6, side * 0.5 + 3, neg_y, swap);
			pos[p][5] = point(door * +6, side * 0.5 + 3, neg_y, swap);
			
			
			// start with first position
			pos_index[p] = 0;
		}
	}
	
	
	private void decideGate(Point[] rats, int p)
	{
		boolean neg_y = id == 2 || id == 3;
		boolean swap  = id == 1 || id == 3;
		double xCoordinate = 0.0;
		double yCoordinate = 0.0;
		// Define point 1 coordinates
		xCoordinate = -0.1*side;
		yCoordinate = 0.25*side;
		Point p1 = point(xCoordinate, yCoordinate, neg_y, swap);
		// Define point 2 coordinates
		xCoordinate = -0.4*side;
		yCoordinate = -0.25*side;
		Point p2 = point(xCoordinate, yCoordinate, neg_y, swap);
		// Define point 1 coordinates
		xCoordinate = 0.1*side;
		yCoordinate = 0.25*side;
		Point p3 = point(xCoordinate, yCoordinate, neg_y, swap);
		// Define point 2 coordinates
		xCoordinate = 0.4*side;
		yCoordinate = -0.25*side;
		Point p4 = point(xCoordinate, yCoordinate, neg_y, swap);
		int count1 = 0;
		int count2 = 0;
	
		for (int i=0; i<rats.length; i++)
		{
			if (rats[i].x < p1.x && rats[i].x > p2.x && rats[i].y < p1.y && rats[i].y > p2.y )
			{
				count1++;
			}
			if (rats[i].x > p3.x && rats[i].x < p4.x && rats[i].y < p3.y && rats[i].y > p4.y )
			{
				count2++;
			}				
		}
		if (count1 > count2)
		{
			pos[p][pos_index[p]] = point(-0.4*side, 0, neg_y, swap);
		}
		else
		{
			pos[p][pos_index[p]] = point(0.4*side, 0, neg_y, swap);
		}
	}
	
	
	private void waitAtGate(Point[] rats, int p)
	{
		int ratsInRange = 0;
		for (int i=0; i<rats.length; i++)
		{
			if (getDistance(pos[p][pos_index[p]], rats[i]) < 10)
			{
				ratsInRange++;
			}
		}
		if (ratsInRange < 3)
		{
			// Wait there only
			pos_index[p] = 0;
		}
	}
	
	private int[] findNofRats(Point[] rats)
	{
		int rats_per[] = new int[16];
		double center = 0;
		double left = -side / 2;
		double bottom = left;
		double right = side / 2;
		double top = right;
		for (int i=0; i<16; i++)
		{
		rats_per[i] = 0;
		}
		for (int i=0; i<rats.length; i++)
		{
		if((rats[i].x <= -side/4 && rats[i].x > left) && (rats[i].y >= side/4 && rats[i].y < top)) rats_per[0]++;
		if((rats[i].x >= -side/4 && rats[i].x <= center) && (rats[i].y >= side/4 && rats[i].y < top)) rats_per[1]++;
		if((rats[i].x >= center && rats[i].x <= side/4) && (rats[i].y >= side/4 && rats[i].y < top)) rats_per[2]++;
		if((rats[i].x >= side/4  && rats[i].x < right) && (rats[i].y >= side/4 && rats[i].y < top)) rats_per[3]++;
		
		if((rats[i].x <= -side/4 && rats[i].x > left) && (rats[i].y >= center && rats[i].y <= side/4)) rats_per[4]++;
		if((rats[i].x >= -side/4 && rats[i].x <= center) && (rats[i].y >= center && rats[i].y <= side/4)) rats_per[5]++;
		if((rats[i].x >= center && rats[i].x <= side/4) && (rats[i].y >= center && rats[i].y <= side/4)) rats_per[6]++;
		if((rats[i].x >= side/4  && rats[i].x < right) && (rats[i].y >= center && rats[i].y <= side/4)) rats_per[7]++;

		if((rats[i].x <= -side/4 && rats[i].x > left) && (rats[i].y >= -side/4 && rats[i].y <= center)) rats_per[8]++;
		if((rats[i].x >= -side/4 && rats[i].x <= center) && (rats[i].y >= -side/4 && rats[i].y <= center)) rats_per[9]++;
		if((rats[i].x >= center && rats[i].x <= side/4) && (rats[i].y >= -side/4 && rats[i].y <= center)) rats_per[10]++;
		if((rats[i].x >= side/4  && rats[i].x < right) && (rats[i].y >= -side/4 && rats[i].y <= center)) rats_per[11]++;
		
		if((rats[i].x <= -side/4 && rats[i].x > left) && (rats[i].y > bottom && rats[i].y <= -side/4)) rats_per[12]++;
		if((rats[i].x >= -side/4 && rats[i].x <= center) && (rats[i].y > bottom && rats[i].y <= -side/4)) rats_per[13]++;
		if((rats[i].x >= center && rats[i].x <= side/4) && (rats[i].y > bottom && rats[i].y <= -side/4)) rats_per[14]++;
		if((rats[i].x >= side/4  && rats[i].x < right) && (rats[i].y > bottom && rats[i].y <= -side/4)) rats_per[15]++;
		}
		return rats_per;
	}
	
	private double[] calculatePiperDist(Point[] rats, Point[][] pipers, int p)
	{
		double dist[] = new double[16];
		double center = 0;
		double left = -side / 2;
		double bottom = left;
		double right = side / 2;
		double top = right;
		dist[0] = Math.sqrt(Math.pow(pipers[id][p].x+side*3/8, 2) + Math.pow(pipers[id][p].y-side*3/8, 2));
		dist[1] = Math.sqrt(Math.pow(pipers[id][p].x+side*1/8, 2) + Math.pow(pipers[id][p].y-side*3/8, 2));
		dist[2] = Math.sqrt(Math.pow(pipers[id][p].x-side*3/8, 2) + Math.pow(pipers[id][p].y-side*3/8, 2));
		dist[3] = Math.sqrt(Math.pow(pipers[id][p].x-side*3/8, 2) + Math.pow(pipers[id][p].y-side*3/8, 2));
		
		dist[4] = Math.sqrt(Math.pow(pipers[id][p].x+side*3/8, 2) + Math.pow(pipers[id][p].y-side*1/8, 2));
		dist[5] = Math.sqrt(Math.pow(pipers[id][p].x+side*1/8, 2) + Math.pow(pipers[id][p].y-side*1/8, 2));
		dist[6] = Math.sqrt(Math.pow(pipers[id][p].x-side*3/8, 2) + Math.pow(pipers[id][p].y-side*1/8, 2));
		dist[7] = Math.sqrt(Math.pow(pipers[id][p].x-side*3/8, 2) + Math.pow(pipers[id][p].y-side*1/8, 2));
		
		dist[8] = Math.sqrt(Math.pow(pipers[id][p].x+side*3/8, 2) + Math.pow(pipers[id][p].y+side*1/8, 2));
		dist[9] = Math.sqrt(Math.pow(pipers[id][p].x+side*1/8, 2) + Math.pow(pipers[id][p].y+side*1/8, 2));
		dist[10] = Math.sqrt(Math.pow(pipers[id][p].x-side*3/8, 2) + Math.pow(pipers[id][p].y+side*1/8, 2));
		dist[11] = Math.sqrt(Math.pow(pipers[id][p].x-side*3/8, 2) + Math.pow(pipers[id][p].y+side*1/8, 2));
		
		dist[12] = Math.sqrt(Math.pow(pipers[id][p].x+side*3/8, 2) + Math.pow(pipers[id][p].y+side*3/8, 2));
		dist[13] = Math.sqrt(Math.pow(pipers[id][p].x+side*1/8, 2) + Math.pow(pipers[id][p].y+side*3/8, 2));
		dist[14] = Math.sqrt(Math.pow(pipers[id][p].x-side*3/8, 2) + Math.pow(pipers[id][p].y+side*3/8, 2));
		dist[15] = Math.sqrt(Math.pow(pipers[id][p].x-side*3/8, 2) + Math.pow(pipers[id][p].y+side*3/8, 2));
		return dist;
	}
	
		
	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{		
		if (numberPasses >= 4)
		{
			this.ratsCountCurrent = rats.length;
			for (int p = 0 ; p != pipers[id].length ; ++p) {
				if (pos_index[p] == 1)
				{
					// Set position at gate, Decide the gate 
					// pick coordinate based on where the player is
					decideGate(rats, p);
				}
			}
		}
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];
			Point dst = pos[p][pos_index[p]];

			// if position is reached
			if (Math.abs(src.x - dst.x) < 0.000001 &&
			    Math.abs(src.y - dst.y) < 0.000001) {
				// Piper has reached position 1 , Wait until he has got a certain number of rats
				if (pos_index[p] == 1 && numberPasses >= 4)
				{
					if (count < 300)
					{
						waitAtGate(rats, p);
						count ++;
					}
					else
					{
						count = 0;
						int rats_per[] = findNofRats(rats);
						double dist[] = calculatePiperDist(rats, pipers, p);
						double ratio[] = new double[16];
						for(int i=0; i<16; i++)
							ratio[i] = rats_per[i] / dist[i];
						int largest_ind_temp = 0;
						double largest_ratio = ratio[0];
						for(int i=1; i<16; i++)
							if (largest_ratio < ratio[i])
								{
								largest_ratio = ratio[i];
								largest_ind_temp = i;
								}
						Random random = new Random();
						if (largest_ind_temp == 0)
						{
							Point temp = new Point(random.nextDouble() * .25 * side - side/2, random.nextDouble() * .25 * side - side/4);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 1)
						{
							Point temp = new Point(random.nextDouble() * .25 * side - side/4, random.nextDouble() * .25 * side - side/4);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 2)
						{
							Point temp = new Point(random.nextDouble() * .25 * side, random.nextDouble() * .25 * side - side/4);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 3)
						{
							Point temp = new Point(random.nextDouble() * .25 * side + side/4, random.nextDouble() * .25 * side - side/4);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 4)
						{
							Point temp = new Point(random.nextDouble() * .25 * side - side/2, random.nextDouble() * .25 * side);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 5)
						{
							Point temp = new Point(random.nextDouble() * .25 * side - side/4, random.nextDouble() * .25 * side);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 6)
						{
							Point temp = new Point(random.nextDouble() * .25 * side, random.nextDouble() * .25 * side);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 7)
						{
							Point temp = new Point(random.nextDouble() * .25 * side + side/4, random.nextDouble() * .25 * side);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 8)
						{
							Point temp = new Point(random.nextDouble() * .25 * side - side/2, random.nextDouble() * .25 * side - side/4);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 9)
						{
							Point temp = new Point(random.nextDouble() * .25 * side - side/4, random.nextDouble() * .25 * side - side/4);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 10)
						{
							Point temp = new Point(random.nextDouble() * .25 * side, random.nextDouble() * .25 * side - side/4);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 11)
						{
							Point temp = new Point(random.nextDouble() * .25 * side + side/4, random.nextDouble() * .25 * side - side/4);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 12)
						{
							Point temp = new Point(random.nextDouble() * .25 * side - side/2, random.nextDouble() * .25 * side - side/2);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 13)
						{
							Point temp = new Point(random.nextDouble() * .25 * side - side/4, random.nextDouble() * .25 * side - side/2);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 14)
						{
							Point temp = new Point(random.nextDouble() * .25 * side, random.nextDouble() * .25 * side - side/2);
							pos[p][1] = temp;
						}
						if (largest_ind_temp == 15)
						{
							Point temp = new Point(random.nextDouble() * .25 * side + side/4, random.nextDouble() * .25 * side - side/2);
							pos[p][1] = temp;
						}	
					}
				}
				if (pos_index[p] == 3)
				{
					numberPasses++;
				}
				// get next position
				if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
				dst = pos[p][pos_index[p]];
			}
			// get move towards position
			if (pos_index[p] == 3 || pos_index[p] == 2)
			{
				moves[p] = moveSlowly(src, dst, pos_index[p] > 1);
			}
			else
			{
				moves[p] = move(src, dst, pos_index[p] > 1);
			}
		}
		
	}

	// This method is follows greedy approach : 
	// The pipers go to the nearest rat , start playing the pipe and come back 
	/*public void play2(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{	
		
		boolean teamPlaying = false;
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			boolean playPiper = false;
			boolean piperStationary = false;
			double minDistance = Double.MAX_VALUE;
			Point src = pipers[id][p];	
			double distance = 0.0;
			//Point dst = null;
			Point dst = pos[p][pos_index[p]];
			// if null then get random position
			//if (dst == null) dst = random_pos[p];
			
			//Calculate if piper should be played or not
			double minX = 0.0;
			double minY = 0.0;
			Point minRat = null;
			for (int i =0; i<rats.length; i++)
			{
				distance = getDistance(src, rats[i]);
				
				if (distance < minDistance)
				{
					minRat = rats[i];
					minX = minRat.x;
					minY = minRat.y;
					minDistance = distance;
				}
			}
			if (minDistance < 3)
			{
				playPiper = true;
				teamPlaying = true;
			}
			
			//If distance between piper and gate is less than 2m, stop the piper
			distance = getDistance(src, pos[p][0]);
			if (distance < 5)
			{
				for (int temp = 0 ; temp < p ; ++temp)
				{
					Point mySrc = pipers[id][temp];
					//If distance between piper and gate is less than 2m, stop the piper
					distance = getDistance(mySrc, pos[p][0]); 
					if (distance < 6  && teamPlaying == true)
					{
						//System.out.println("Reached here at least ");
						//if (pipers_played[id][temp] == true)
						//{
							//System.out.println("Reached here man ");
							playPiper = false;
							piperStationary = true;
							break;
						//}
					}
				}
			} 
			if (pos_index[p] == 1)
			{	
				// If the minimum distance is less than 10, it means that rat is within the piper's range,
				// Now it will get automatically attracted towards the piper
				if (minDistance < 3)
				{
					//Increment pos index
					pos_index[p] = pos_index[p] + 1;
					dst = pos[p][pos_index[p]];
				}
				else
				{
					// Continue looking for the rat unless one rat is found
					// Sweeping
					// Update the destination
					dst = new Point(minX, minY);
				}
			}
			else
			{
				if (Math.abs(src.x - dst.x) < 0.000001 && Math.abs(src.y - dst.y) < 0.000001) {
					// get next position
					if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
					dst = pos[p][pos_index[p]];
					
					if (pos_index[p] == 1)
					{
						dst = new Point(minX, minY);
					}
				}
				if (piperStationary == true)
				{
					dst = new Point(src.x, src.y);
				}
				
			}
			// If the position is reached,  get move towards position
			//moves[p] = move(src, dst, pos_index[p] > 1);
			if (src == null)
			{
				System.out.println("Source is null ");
			}
			if (dst == null)
			{
				System.out.println("Destination is null ");
			}
			
			moves[p] = move(src, dst, playPiper);
			//moves[p] = move(src, dst, true);
		}
	} */
}
