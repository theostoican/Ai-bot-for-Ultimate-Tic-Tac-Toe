// // Copyright 2016 theaigames.com (developers@theaigames.com)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * BotStarter class
 * 
 * Magic happens here. You should edit this file, or more specifically the
 * makeTurn() method to make your bot do more than random moves.
 * 
 * @author Jim van Eeden <jim@starapple.nl>
 */

public class BotStarter {
	
	public static int startMinimax = 0;
	public static int roundNr;
	public static double MaxVal = 10000000000000000000.0;
	public static double MinVal = -10000000000000000000.0;
	public static int hashMax = 35500;
	public static HashMap<Board, PairNew> oldHashTable = new LinkedHashMap<Board, PairNew>(hashMax);
	public static HashMap<Board, PairNew> newHashTable = new LinkedHashMap<Board, PairNew>(hashMax);
	public static int DEPTH = 7;
	public Move ret ;
	
	public int cntRight = 0;
	public int sumRight = 0;
	public int sumWrong = 0;
	int cntWrong = 0;
	
	class Comparator implements java.util.Comparator<Pair> {

		@Override
		public int compare(Pair o1, Pair o2) {
			if(o1.first() < o2.first())
				return 1;
			else if(o1.first() > o2.first())
				return 1;
			return 0;
		}	
	}
	class Comparator2 implements java.util.Comparator<State> {

		@Override
		public int compare(State o1, State o2) {
			if(o1.getScore() < o2.getScore())
				return -1;
			else if(o1.getScore() > o2.getScore())
				return 1;
			return 0;
		}	
	}
	
	class ComparatorInv2 implements java.util.Comparator<State> {

		@Override
		public int compare(State o1, State o2) {
			if(o1.getScore() > o2.getScore())
				return -1;
			else if(o1.getScore() < o2.getScore())
				return 1;
			return 0;
		}	
	}

	private void sortByHash(State[] nodes, Board board, Parameters param, int perspective) {
		
		List<Move> moves = board.getAvailableMoves();
		int sz = moves.size();
		
		for(int i = 0 ; i < sz; ++i) {
			
			nodes[i] = new State();
			Move move = moves.get(i);
			nodes[i].setBoard(board.applyMove(move, param));
			nodes[i].setMove(new Move(move.getX(), move.getY()));
		}
		
		for(int i = 0; i < sz; i++) {
			
			PairNew p = oldHashTable.get(nodes[i].getBoard());
			
			if(p == null) //daca unul nu se gaseste teoretic nu ar trebuie sa se gaseasca niciunul state in hash
				return ;
			
			if(p != null) 
				nodes[i].setScore(p.getScore());	
		}
		
		
		if(perspective == 1)
			Arrays.sort(nodes, new ComparatorInv2());//descrescatoare in ordinea punctajului, pentru maxi
		else 
			Arrays.sort(nodes, new Comparator2());//crecatoare, de la cele mai proaste miscari pentru mine, pt mini

	}
	
	public Pair maxi1(Board board, Parameters param, int depth, double alpha, double beta, int maxMoves) {
		
		if (depth == 0 || board.ended())
			return new Pair(board.eval1(param), new Move());//evaluez tot timpul din perspectiva mea
		
		//board.SortMoves(param);//sortare cu greedy
		
		List<Move> moves = board.getAvailableMoves();
		int sz = moves.size();
		
		State[] nodes = new State[sz];
		sortByHash(nodes, board, param, 	1);//vreau in ordine descrescatoare

		
		Move bestMove = nodes[0].getMove();
		
		if(depth == DEPTH) 
			ret = bestMove;
		
		double bestScore = MinVal;
		
		for (int i = 0 ; i < maxMoves && i < sz ; ++i) {
			
			Move move = nodes[i].getMove();
			Board clone = nodes[i].getBoard();
			
			Pair pair = mini1(clone, param.inverse(), depth - 1, alpha, beta, maxMoves);
		
			
			if(pair.first() > bestScore) {
				bestScore = pair.first();
				bestMove = move;
			}
			
			if(bestScore > alpha)  
				alpha = bestScore;
			
			if (alpha >= beta) 
				break;
		}
		

		if(depth == DEPTH) 
			if(ret.getX() == bestMove.getX() && ret.getY() == bestMove.getY()) {
				cntRight++;
				sumRight++;
			}
			else {
				cntWrong++;
				sumWrong++;
			}
		
		if(depth >= 2)
			newHashTable.put(board, new PairNew(depth, bestScore));//punctajul bagat e tot timpul din perspectiva mea
		return new Pair(bestScore, bestMove);
	}
	
	public Pair mini1(Board board, Parameters param, int depth, double alpha, double beta, int maxMoves) {
		
		if (depth == 0 || board.ended())
			return new Pair (board.eval1(param.inverse()), new Move());//evaluez tot timpul din perspectiva mea
		
		
		List<Move> moves = board.getAvailableMoves();
		int sz = moves.size();
		
		State[] nodes = new State[sz];
		
		sortByHash(nodes, board, param, 2);//2-ordine descrescatoare
		
		Move bestMove = nodes[0].getMove();
		double minScore = MaxVal;
		
		for (int i = 0 ; i < maxMoves && i < sz ; ++i) {
			
			Move move = nodes[i].getMove();
			Board clone = nodes[i].getBoard();
			Pair pair = maxi1(clone, param.inverse(), depth - 1, alpha, beta, maxMoves);
			
			//System.out.println(pair.first() + " "  + move.getX() + " "  + move.getY());
			if(minScore > pair.first()) {
				minScore = pair.first();
				bestMove = move;
			}
			
			if(minScore < beta)
				beta = minScore;
			
			if (beta <= alpha)
				break;
		}
		
		if(depth >= 2)
			newHashTable.put(board, new PairNew(depth, minScore));
		
		return new Pair(minScore, bestMove);
	}
	
	public Pair maxi2(Board board, Parameters param, int depth, double alpha, double beta, int maxMoves) {
		
		if (depth == 0 || board.ended()) 
			return new Pair(board.eval2(param), new Move());
		
		List<Move> moves = board.getAvailableMoves();
	    int sz = moves.size();
	     //board.SortMoves(param);
	
		Move bestMove = board.getAvailableMoves().get(0);
		double bestScore = MinVal;
		
		for (int i = 0 ; i < maxMoves && i < sz ; ++i) {
			
			//Move move = nodes[i].getMove();
			Move move = board.getAvailableMoves().get(i);
			Board clone = board.applyMove(move, param);
			Pair pair = mini2(clone, param.inverse(), depth - 1, alpha, beta, maxMoves);
			
			if(bestScore < pair.first()) {
				bestScore = pair.first();
				bestMove = move;
			}
			
			if(bestScore > alpha)  
				alpha = bestScore;
			
			if (alpha >= beta) 
				break;
		}
		return new Pair(bestScore, bestMove);
	}
	
	public Pair mini2(Board board, Parameters param, int depth, double alpha, double beta, int maxMoves) {
		//din perspective adversarului
		if (depth == 0 || board.ended())
			return new Pair(board.eval2(param.inverse()), new Move());
		
		int sz = board.getAvailableMoves().size();
		
		//board.SortMoves(param);
		double minScore = MaxVal;
		Move bestMove = board.getAvailableMoves().get(0);
		
		//beta = Double.MAX_VALUE;
		for (int i = 0 ; i < maxMoves && i < sz ; ++i) {
			
			//Move move = nodes[i].getMove();
			Move move = board.getAvailableMoves().get(i);
			
			//Board clone = nodes[i].getBoard();
			Board clone = board.applyMove(move, param);
			
			Pair pair = maxi2(clone, param.inverse(), depth - 1, alpha, beta, maxMoves);
			
			
			if(minScore > pair.first()) {
				minScore = pair.first();
				bestMove = move;
			}
			
			if(minScore < beta)
				beta = minScore;
			
			if (beta <= alpha)
				break;	
		}
		
		return new Pair(minScore, bestMove);
	}
	
	
	private void cleanMiniMax1(Parameters params) {

	   oldHashTable.clear();
	   Iterator it = newHashTable.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Board, PairNew> pair = (Map.Entry)it.next();
	        oldHashTable.put((Board)pair.getKey(), (PairNew)pair.getValue());
	        it.remove(); 
	    }
	}

	public Move makeTurn(Field field) {

		/* You don't have to know ant convention for param, just give 5 random numbers different one from each other and it's okay
		 * We convert their field into our Board with these convention.*/
	
		Parameters param = new Parameters( 8 ,9 , 3 , 4, 5);
		Board board = field.convertToOurRepresentation(param);
		int depth1 = 7;
		int depth2 = 7;
		int playGames = 10;
		
		int maxMovesAnalysed = Integer.MAX_VALUE;
		
		int maxMovesAnalysed2 = Integer.MAX_VALUE;
	
		int win1 = 0;
		int win2 = 0;
		int draws = 0;
		int movesMade = 0;
		
	
		Move m = null;
		Random r = new Random();
		
		Chronometer ch1 = new Chronometer();
		Chronometer ch2 = new Chronometer();
		
		long time1 = 0;
		long time2 = 0;
		
		
		for(int i = 0 ; i < playGames; ++i) {	
			int cnt = 0; 
			while(board.ended() == false ) {
				if(cnt % 2 == 0) {
					
					ch1.start();
					
					if(cnt == 0) 
						m = board.getAvailableMoves().get(r.nextInt(board.getAvailableMoves().size() - 1));
					else {
						Pair m2;
						
						if(cnt < 16){
							m2 = maxi1(board, param, depth1 - 2, MinVal, MaxVal, maxMovesAnalysed);//zero
							cleanMiniMax1(param);
						}
						
						else {
							
							if(board.getAvailableMoves().size() > 9)
								m2 = maxi1(board, param, depth1 - 1, MinVal, MaxVal, maxMovesAnalysed);
							else 
								m2 = maxi1(board, param, depth1, MinVal, MaxVal, maxMovesAnalysed);
							cleanMiniMax1(param);
						}
						
						
						
						m = m2.second();
					}
					ch1.stop();
					
					time1 += ch1.getTime();
					
				} else {
					ch2.start();
					if(cnt == 1)
						m = board.getAvailableMoves().get(r.nextInt(board.getAvailableMoves().size() - 1));
					else {
						
						Pair m2 = null;
						
						
						if(cnt < 16)
							m2 = maxi2(board, param, depth2 - 2, MinVal, MaxVal, maxMovesAnalysed);//zero
						else {
							
							if(board.getAvailableMoves().size() > 9)
								m2 = maxi2(board, param, depth2 - 1, MinVal, MaxVal, maxMovesAnalysed);
							else 
								m2 = maxi2(board, param, depth2 , MinVal, MaxVal, maxMovesAnalysed);
						}
						
						
						m = m2.second();	
					}
					ch2.stop();
					
					time2 += ch2.getTime();
				}
				board = board.applyMove(m, param);
				param = param.inverse();
				
				cnt++;
				/*
				
				System.out.println("Move: " + m.getX() + " " + m.getY());
				if(cnt % 2 == 1)
					board.printCurentState();
				else  {
					
					board.setParameters(param.inverse());
					board.printCurentState();
					board.setParameters(param.inverse());
				}
				*/
				
			
			}
			movesMade += cnt;
			
			if(cnt % 2 == 1)
				param = param.inverse();
			
			if(board.gameIsWonBy(param) == param.getMySq())
				win1++;
			if(board.gameIsWonBy(param) == param.getEnemySq())
				win2++;
			if(board.gameIsWonBy(param) == param.getEmptySq())
				draws++;
			
			System.out.println("Won by: " +  board.gameIsWonBy(param));
			System.out.println("Time: player " + time1 + " " + time2 );
			System.out.println("First move wrong / right : " + cntWrong  + " " + cntRight);
			
			param = new Parameters( 8 , 9 , 3 , 4, 5);
			board.clearBoard(param);
	
		}
			
		
		System.out.println("Results:\n" + "Player1: " + win1 + "\nPlayer2: " + win2 + "\nDraws: " + draws );
		System.out.println("Tiem total player1 " + time1);
		System.out.println("Time total player2 " + time2);
		System.out.print("Time total :  ");

		System.out.println(time1 + time2);
		System.out.println("Total wrong / right : " + sumWrong + " " + sumRight);
		
		
		
		int nr = board.getAvailableMoves().size();
		
		if(field.getRound() < 16)
			 return  maxi1(board, param, depth1 - 2, MinVal,  MaxVal, maxMovesAnalysed).second();
		else {
			
			if(board.getAvailableMoves().size() > 9)
				return maxi1(board, param, depth1 - 1, MinVal, MaxVal, maxMovesAnalysed).second();
			else 
				return maxi1(board, param, depth1 , MinVal, MaxVal, maxMovesAnalysed).second();
		}
		
	}

	public static void main(String[] args) {
		BotParser parser = new BotParser(new BotStarter());
		parser.run();
	}
}
