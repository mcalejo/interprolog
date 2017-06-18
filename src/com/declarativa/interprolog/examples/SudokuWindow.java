/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.examples;
import com.declarativa.interprolog.*;

import javax.swing.*;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

/** A window showing a Sudoku puzzle board, with the ability to edit it, save it to a Prolog text file, restore (open) it from either a
file or PROLOG memory, and to solve it. launch with java ... com.declarativa.interprolog.examples.SudokuWindow */
@SuppressWarnings("serial")
public class SudokuWindow extends JFrame {
	SudokuModel model;
	PrologEngine engine;
	public SudokuWindow(PrologEngine e){
		engine=e;
		engine.consultFromPackage("Sudoku.P",SudokuWindow.class);
		ObjectExamplePair[] examples = {getNxNintArrayExample(), singleCellValue(), arrayOfCellValue()};
		if(!engine.teachMoreObjects(examples)) throw new RuntimeException("failed to teach object examples");

		// build menus, which call Prolog goals to do most of the work
		setJMenuBar(buildMenus()); 
		
		// Build the JTable and its model, whose data will flow to/from Prolog
		model = new SudokuModel();
		JTable board = new SudokuBoard(model);
		
		getContentPane().add(BorderLayout.CENTER,board);
		pack(); setVisible(true);
	}
	
	/** An ObjectExamplePair illustrating how to pass around a matrix of basic type values */
  	static ObjectExamplePair getNxNintArrayExample(){
		int[][] cells1 = new int[9][9];
		int[][] cells2 = new int[cells1.length][cells1.length];
		for(int i=0;i<cells2.length;i++)
			for(int j=0;j< cells2.length;j++){
				cells1[i][j]=1;
				cells2[i][j]=2; 
			}
		return new ObjectExamplePair("NxNintArray",cells1,cells2);
  	}
  	
  	/** A class illustrating how to define a custom data bundle to be passed from Prolog  */
  	static class CellValue implements Serializable{
  		int X,Y,N; // 0..N-1
  		/** 1..N */
  		int getMyX(){return X-1;}
  		int getMyY(){return Y-1;}
  	}
  	
  	static ObjectExamplePair singleCellValue(){
  		return new ObjectExamplePair(new CellValue());
  	}
  	static ObjectExamplePair arrayOfCellValue(){
  		return new ObjectExamplePair("CellArray",new CellValue[0]);
  	}
  	
	JMenuBar buildMenus(){
		JMenuBar mb; 
		mb = new JMenuBar(); setJMenuBar(mb);
		JMenu fileMenu = new JMenu("Puzzle"); 
        mb.add(fileMenu);
		JMenuItem newPuzzle = new JMenuItem("New");
		fileMenu.add(newPuzzle);
		newPuzzle.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (!engine.deterministicGoal("newPuzzle")) throw new RuntimeException("Prolog goal should not have failed!");
				model.clean();
			}
		});
		JMenuItem openPuzzle = new JMenuItem("Open File");
		fileMenu.add(openPuzzle);
		openPuzzle.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				String nome,directorio; File filetoopen=null;
				FileDialog d = new FileDialog(SudokuWindow.this,"Open puzzle...");
				d.setVisible(true);
				nome = d.getFile(); directorio = d.getDirectory();
				if (nome!=null) {
					filetoopen = new File(directorio,nome);
					if (!engine.deterministicGoal("newPuzzle")) throw new RuntimeException("Prolog newPuzzle goal should not have failed!");
					if (engine.consultAbsolute(filetoopen)){
						Object[] bindings = engine.deterministicGoal("openPuzzle('"+filetoopen.getAbsolutePath()+"',Array)","[Array]");
						if (bindings==null) 
							throw new RuntimeException("Prolog openPuzzle goal should not have failed!");
						model.setCells((int[][])bindings[0]);
					} else throw new RuntimeException("Prolog consult should not have failed!");
				}			
			}
		});
		JMenuItem loadPuzzle = new JMenuItem("Load from PROLOG");
		fileMenu.add(loadPuzzle);
		loadPuzzle.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Object[] bindings = engine.deterministicGoal("loadPuzzle(Array)","[Array]");
				if (bindings==null) 
					throw new RuntimeException("Prolog loadPuzzle goal should not have failed!");
				model.setCells((int[][])bindings[0]);
			}
		});
		JMenuItem savePuzzle = new JMenuItem("Save");
		fileMenu.add(savePuzzle);
		savePuzzle.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				String nome,directorio; File filetosave=null;
				FileDialog d = new FileDialog(SudokuWindow.this,"Save puzzle...",FileDialog.SAVE);
				d.setVisible(true);
				nome = d.getFile(); directorio = d.getDirectory();
				if (nome!=null) {
					filetosave = new File(directorio,nome);
					if (!engine.deterministicGoal("assertBoard(Matrix), savePuzzle('"+filetosave.getAbsolutePath()+"')" ,"[Matrix]",new Object[]{model.cells}))
						throw new RuntimeException("Prolog goal should not have failed!");
				}			
			}
		});
		JMenuItem solvePuzzle = new JMenuItem("Solve");
		solvePuzzle.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Object[] bindings = engine.deterministicGoal(
					"assertBoard(Matrix), resolve(L), prepareSolutionCells(L,SL), ipObjectSpec('CellArray',SL,Solution)",
					"[Matrix]",new Object[]{model.cells},
					"[Solution]");
				if (bindings==null){
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(SudokuWindow.this,"Puzzle has no solution","Prolog failure",JOptionPane.ERROR_MESSAGE);
				} else model.setCells((CellValue[])bindings[0]);
			}
		});
		fileMenu.add(solvePuzzle);
		return mb;
	}
	
	/** A visible Sudoku board */
	static class SudokuBoard extends JTable{
		public SudokuBoard(TableModel m){
			super(m);
			setCellSelectionEnabled(true);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			TableColumnModel tcm = getColumnModel();
			Font font = getFont().deriveFont((float)20.0);
			setFont(font);
			int width = getFontMetrics(font).getMaxAdvance();
			setDefaultRenderer(Integer.class,new MyCellRenderer());	
			for(int c=0;c<tcm.getColumnCount();c++){
				((TableColumn)tcm.getColumn(c)).setMaxWidth(width);
			}
			setRowHeight(width);
		}
		public void paint(Graphics g){
			super.paint(g);
			g.setColor(Color.black);
			for(int r=3;r<9;r+=3){
				int Y = r*getRowHeight();
				g.drawLine(0,Y-1, getWidth()-1, Y-1);
			}
			for(int c=3;c<9;c+=3){
				int X = getCellRect(0,c,true).x;
				g.drawLine(X-1,0,X-1,getHeight()-1);
			}
		}
		/** Hack to set the font during cell editing */
		public TableCellEditor getDefaultEditor(Class<?> columnClass){
			DefaultCellEditor T = (DefaultCellEditor)super.getDefaultEditor(columnClass);
			T.getComponent().setFont(getFont());
			return T;
		}
	}
	
	/** A JTable renderer to get the wanted look */
	static class MyCellRenderer extends DefaultTableCellRenderer{
		public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column){
			Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
			if (table.getModel().isCellEditable(row,column)) c.setForeground(Color.black);
			else c.setForeground(Color.blue);
			((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
			c.setFont(table.getFont());
			return c;
		}
	}
	
	/** JTable model for the Sudoku board */		
	static class SudokuModel extends AbstractTableModel implements Serializable{
		private static final long serialVersionUID = -5561829461240275409L;
		/** cells[X][Y] means cell in line X (0..N-1, bottom to top, opposite of JTable's convention), 
			column Y (0..N-1, left to right). 0 means empty cell. */
		int[][] cells; 
		/** true means cell is part of the problem */
		boolean[][] given; 
		
		SudokuModel(){
			cells = new int[9][9]; 
			given = new boolean[getRowCount()][getColumnCount()];
			clean();
		}
		void clean(){
			for(int i=0;i<getRowCount();i++)
				for(int j=0;j< getColumnCount();j++){
					cells[i][j]=0; given[i][j]=false;
				}
			fireTableDataChanged();
		}
		/** Given a JTable row ordinate, returns index in cells/given */
		int cellY(int tableRow){return getRowCount()-1-tableRow;}
		public int getRowCount(){return cells.length;}
		public int getColumnCount(){return cells[0].length;}
		public Class<?> getColumnClass(int columnIndex){return Integer.class;}
		public Object getValueAt(int row, int column){
			int value = cells[cellY(row)][column];
			if (value==0) return null;
			else return new Integer(value);
		}
		public boolean isCellEditable(int rowIndex, int columnIndex){
			if (given[cellY(rowIndex)][columnIndex]) return false;
			else return true; 
		}
		public void setValueAt(Object aValue, int rowIndex, int columnIndex){
			int value = 0;
			if(aValue!=null) value = ((Integer)aValue).intValue();
			int oldValue = cells[cellY(rowIndex)][columnIndex];
			// the following would be better done in a TableCellEditor, see for example
			// http://java.sun.com/docs/books/tutorial/uiswing/components/table.html#validtext 
			if (value<0||value>getRowCount()) value=oldValue;
			cells[cellY(rowIndex)][columnIndex] = value;
		}
		/** Define the board content to be the whole cells array. This is one of the methods used to put data from Prolog */
		public void setCells(int[][] cells){
			if (cells!=null) {
				this.cells=cells;
				for(int i=0;i<getRowCount();i++)
					for(int j=0;j< getColumnCount();j++){
						if(cells[i][j]!=0) given[i][j]=true; else given[i][j]=false;
					}				
				fireTableDataChanged();
			}
		}
		/** Change a subset of the board. This is one of the methods used to put data from Prolog */
		public void setCells(CellValue[] values){
			if(values!=null){
				for(int c=0;c<values.length;c++){
					CellValue cv = values[c];
					cells[cv.getMyY()][cv.getMyX()]=cv.N;
				}
				fireTableDataChanged();
			}
		}
  	}
  	
  	/** launch with j com.declarativa.interprolog.examples.SudokuWindow */
	public static void main(String[] args){
		// build an engine; no file path is given, assuing that either an environnment variable or interprolog.defs file exists
		// (see AbstractPrologEngine's constructor documentation)
		final XSBSubprocessEngine engine = new XSBSubprocessEngine();
		SudokuWindow w = new SudokuWindow(engine);
		// Let the user exit:
		w.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				engine.shutdown();
				System.exit(0);
			}
		});
		// This is nonessencial: create a console window just to let the user inspect and hack the Prolog side directly as well
		// new XSBSubprocessEngineWindow(engine);
	}
}