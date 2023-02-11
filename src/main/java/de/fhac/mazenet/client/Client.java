package de.fhac.mazenet.client;

import java.io.IOException;

import java.net.Socket;

import java.util.ArrayList;


import java.util.List;
import javax.xml.bind.UnmarshalException;
import de.fhac.mazenet.server.game.Board;
import de.fhac.mazenet.server.game.Position;
import de.fhac.mazenet.server.generated.*;
import de.fhac.mazenet.server.networking.*;



public class Client {
	static Socket clientsocket ;

	static XmlInputStream in ;
	static XmlOutputStream out ;
	static int id ;
	static boolean gamestatus = true;
	static int i = 0 ;
	static Position CurentPosition;
	static Board b ;
	static Treasure t ;
	static MoveMessageData move ;
	static PositionData toTreasure ;
	static Position p ; 
	static List<Position> list ;
	static List<Position> l ;
	public static void main(String[]args) {

		try {


			//			        	SSLSocketFactory socketBuilder = (SSLSocketFactory) SSLSocketFactory.getDefault();
			//			            SSLSocket clientsocket = (SSLSocket) socketBuilder.createSocket("localhost", 5123);

			clientsocket = new Socket("localhost",5123);
			in = new XmlInputStream(clientsocket.getInputStream());
			out = new XmlOutputStream(clientsocket.getOutputStream());





			System.out.println(clientsocket.getRemoteSocketAddress());


			//giving the name as an input

			//	String name  = args[0];
			//player name
			String name  = "Roboter";

			//setting up the login message
			MazeCom Login = new MazeCom();
			Login = MazeComMessageFactory.createLoginMessage(name);
			out.write(Login);	

			//starting the game
			while(gamestatus)	{

				MazeCom fromserver = in.readMazeCom() ;



				MazeComMessagetype reply =fromserver.getMessagetype() ; 
				System.out.println(reply); 
				play(fromserver);

			}

		} catch (IOException e) {

			e.printStackTrace();

		} catch (UnmarshalException e) {

			e.printStackTrace();
		}
	}

	public static void play(MazeCom m) throws IOException {

		switch (m.getMessagetype()) {

		case LOGINREPLY:
			id = m.getLoginReplyMessage().getNewID();
			break;


		case ACCEPT :
			System.out.println(m.getAcceptMessage().getErrortypeCode());
			Errortype e= m.getAcceptMessage().getErrortypeCode();
			switch (e) {
			case AWAIT_LOGIN:
				break;
			case AWAIT_MOVE:
				break;
			case ERROR:
				break;
			case ILLEGAL_MOVE:
				System.out.println(b.validateTransition(move,id));
				break;
			case NOERROR:
				break;
			case TIMEOUT:
				break;
			case TOO_MANY_TRIES:
				break;
			default:
				break;
			}
			break;



		case AWAITMOVE:

            //the board
			b = new Board(m.getAwaitMoveMessage().getBoard());
			//the treasure positiondata
			t =m.getAwaitMoveMessage().getTreasureToFindNext();
			//the treasure position
			toTreasure= b.findTreasure(t);
			//the message to be sent
			MazeCom maze = new MazeCom();
			//the current position
			CurentPosition = b.findPlayer(id);
			//the object to be sent
			ObjectFactory obj = new ObjectFactory();
			//the move to be sent
			move= obj.createMoveMessageData();

           

            // list of all possible shitcard positions
			list = new ArrayList<Position>();

			for(int i = 1; i<6 ; i=i+2) {
				Position p1 = new Position(0,i);
				Position p2 = new Position(6,i);
				Position p3 = new Position(i,6);
				Position p4 = new Position(i,0);
				list.add(p4);list.add(p3);list.add(p2);list.add(p1);

			}


		

            //romoving the forbiden
			list.remove(b.getForbidden());

			//			System.out.println("forbiden position :"+forbiden);
			//			System.out.println("shift position list :"+list);
			
			//all reachable possitions to treasure
			List<Position> l = b.getAllReachablePositions(toTreasure);
			

			mainLoop:
				
				//testing all possible moves 1 times each
				for (int i = 0; i < list.size(); i++) {
					toTreasure= b.findTreasure(t);
					
					
					//if the treasure is in a shift card
					if(toTreasure == null) {
						move.setNewPinPos(CurentPosition);
						move.setShiftPosition(list.get(0));
						move.setShiftCard(b.getShiftCard());
						
						
						maze.setMoveMessage(move);
						maze.setMessagetype(MazeComMessagetype.MOVE);
						break mainLoop ;
			           }
					
					
					
					
					
					move.setNewPinPos(toTreasure);
					move.setShiftPosition(list.get(i));
					move.setShiftCard(b.getShiftCard());
					Board tb = b.fakeShift(move);
					l = tb.getAllReachablePositions(toTreasure);
					tb.findTreasure(t);
					if (l.contains(CurentPosition)== true ){
						move.setNewPinPos(tb.findTreasure(t));
						if(b.validateTransition(move,id) ==true) {
							System.out.println("one");
							System.out.println(b.getForbidden()+"//"+move.getShiftPosition());
							maze.setMoveMessage(move);
							maze.setMessagetype(MazeComMessagetype.MOVE);
							break;
						}
					}
					else {

						//testing all possible moves 6 times
						for (i = 0; i < list.size(); i++) {
							Board br = b ;

							for (int x = 0; x < 6; x++) {
								CurentPosition = br.findPlayer(id);
								toTreasure= br.findTreasure(t);
								if(toTreasure == null) {
									break ;
								}


								move.setNewPinPos(CurentPosition);
								move.setShiftPosition(list.get(i));
								move.setShiftCard(br.getShiftCard());
								br = br.fakeShift(move);
								l = br.getAllReachablePositions(toTreasure);

								if (l.contains(CurentPosition)== true) {
									//multiplayer prob here
									move.setNewPinPos(b.findPlayer(id));
									move.setShiftPosition(list.get(i));
									move.setShiftCard(b.getShiftCard());
									if(b.validateTransition(move,id)) {
										System.out.println("two");
										System.out.println(b.getForbidden()+"//"+move.getShiftPosition());
										maze.setMoveMessage(move);
										maze.setMessagetype(MazeComMessagetype.MOVE);
										break mainLoop;
									}
								}
								//testing 2 moves
								if(x==5 && i ==10 ){
									for( int a = 0; a<list.size();a++) {
										br = b ;
										CurentPosition = br.findPlayer(id);
										toTreasure= br.findTreasure(t);
										if(toTreasure == null) {
											break;
										}

										for(int c=0;c<list.size();c++) {

											move.setNewPinPos(CurentPosition);
											move.setShiftPosition(list.get(a));
											move.setShiftCard(br.getShiftCard());
											br = br.fakeShift(move);



											move.setNewPinPos(CurentPosition);
											move.setShiftPosition(list.get(c));
											move.setShiftCard(br.getShiftCard());
											br = br.fakeShift(move);
											l = br.getAllReachablePositions(toTreasure);

											if (l.contains(CurentPosition)== true) {
												move.setNewPinPos(b.findPlayer(id));
												move.setShiftPosition(list.get(a));
												move.setShiftCard(b.getShiftCard());
												if (b.validateTransition(move,id)== true) {
													System.out.println("tree");
													System.out.println(b.getForbidden()+"//"+move.getShiftPosition());
													maze.setMoveMessage(move);
													maze.setMessagetype(MazeComMessagetype.MOVE);
													break mainLoop;
												}
											}
											
											//changing player position to retest
											if(a==10 && c ==10) {
												System.out.println("ici");
												move.setNewPinPos(b.findPlayer(id));
												List<Position> listp = br.getAllReachablePositions(CurentPosition);
												move.setShiftPosition(listp.get(listp.size()-1));
												move.setShiftCard(b.getShiftCard());
												System.out.println(b.getForbidden()+"//"+move.getShiftPosition());
												System.out.println("four");
												b.validateTransition(move,id);
											
												
												if(b.validateTransition(move,id)==true) {
												maze.setMoveMessage(move);
												maze.setMessagetype(MazeComMessagetype.MOVE);
												break mainLoop;
												}
												else {
													move.setNewPinPos(b.findPlayer(id));
													move.setShiftPosition(listp.get(listp.size()-1));
													move.setShiftCard(b.getShiftCard());
												}
											}


										
									








								}
							}
						}

					}
				}


		}

	}

	try {
		out.write(maze);
	} catch (IOException er) {

		er.printStackTrace();
	}









	break;



case CONTROLSERVER:
	break;

case DISCONNECT:
	System.out.println("Client shuting down");
	gamestatus = false ;
	break;

case GAMESTATUS:
	if(m.getGameStatusMessage().getFoundTreasures().size() == 24) {
		gamestatus =false;
	}
	break;

case LOGIN:
	break;

case MOVE:
	break;

case MOVEINFO:
	m.getMoveInfoMessage().getPlayerId();
	break;

case WIN:
	System.out.println(m.getWinMessage().getBoard());
	System.out.println(m.getWinMessage().getStatistics());
	gamestatus = false ;
	break;

default:
	break;

}
}


}