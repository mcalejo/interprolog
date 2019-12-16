/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.declarativa.interprolog.TermModel;

@SuppressWarnings({ "unchecked", "serial" }) 
public class TermTreePane extends JPanel{
	JList<TermModel>[] lists;
	TermTreeModel model;
	Object[] levelTitles;
	TermTreePane(TermTreeModel m, Object[] levelTitles){
		setLayout(new BorderLayout());
		if (m==null) 
			throw new RuntimeException("Null model in TermTreeWindow");
		int nLevels = m.depth();
		if (nLevels<2) 
			throw new RuntimeException("Bad model in TermTreeWindow:"+m);
		if (levelTitles==null) levelTitles = new String[0];
		this.levelTitles = levelTitles;
		
		lists = new JList[nLevels];
		model=m;
		//String[] dummy = {"     "};
		for (int level=0; level<nLevels; level++){
			//final JList list = (level==0?new JList(dummy0):new JList(dummy));
			final JList<TermModel> list = new JList<TermModel>();
			lists[level] = list;
			if (level<nLevels-1) list.addListSelectionListener(new ListHandler(level));
	     	list.addMouseListener(new MouseAdapter() {
	     		public void mouseClicked(MouseEvent e) {
	         		if (e.getClickCount() == 2) {
	             		int index = list.locationToIndex(e.getPoint());
	             		TermListModel lm = (TermListModel)(list.getModel());
	             		if (index != -1) new TermModelWindow(lm.terms[index]);
	     			}
	     		};
	     	});

		}
		lists[0].setModel(m.topList());
		if (nLevels>2){
			JSplitPane current = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, 
				paneForList(0), paneForList(1));
			current.setDividerLocation(0.5);
			for (int level=2;level<nLevels-1;level++){
				current = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, current, paneForList(level)); 
				current.setDividerLocation((float)level/(nLevels-1));
			}
			add("Center",current);
		}
		else add("Center",paneForList(0));
	}
	
	JComponent paneForList(int L){
		JPanel temp = new JPanel(new BorderLayout());
		temp.add("Center",new JScrollPane(lists[L]));
		if (L>levelTitles.length-2)
			temp.add("North",new JLabel(" "));
		else
			temp.add("North",new JLabel(levelTitles[L+1].toString()));
		return temp;
	}
	class ListHandler implements ListSelectionListener{
		int position;
		ListHandler(int i){position=i;}
		public void valueChanged(ListSelectionEvent e){ // repaint() needed !
			JList<TermModel> list = (JList<TermModel>)(e.getSource());
			
			if (!e.getValueIsAdjusting() & list.getSelectedIndex()!=-1){
				lists[position+1].setModel(findModel(position).topList());
				clearRightLists();
				repaintRight();
			}
		}
		
		void clearRightLists(){ 
			for(int level=position+2; level<lists.length; level++){
				lists[level].setListData(new TermModel[0]);
			}
		}
		void repaintRight(){
			for(int level=position+1; level<lists.length; level++){
				lists[level].repaint();
				//lists[level].clearSelection();
				//lists[level].revalidate();
			}
		}
		
		TermTreeModel findModel(int position){ // all JLists have a selection up to position
			TermTreeModel temp = model.children[lists[0].getSelectedIndex()];
			for (int level=1; level<=position; level++){
				temp=temp.children[lists[level].getSelectedIndex()];
			}
			return temp;
		}
	}
}
