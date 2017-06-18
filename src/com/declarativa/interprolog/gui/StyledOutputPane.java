package com.declarativa.interprolog.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.rtf.RTFEditorKit;


public class StyledOutputPane extends JTextPane{
	private static final long serialVersionUID = 1L;

	Action copyAction = new AbstractAction("Copy"){
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			copyAsRTF();
		}	
	};

	public StyledOutputPane(){
		super(new DefaultStyledDocument());
	}
	
	/**
	 * This implementation does nothing.
	 */
	public void initializeOutputStyles() {}
	
	public void append(final String str, final Style a){
		// A test made to ensure Prolog clients can message this at will even after a window is closed
		if (!isDisplayable())
			return;
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				try {
					getDocument().insertString(getDocument().getLength(), str, a);
					/* Buggy (at least on Mac): returns always the default style!
					System.out.println("insertString:"+str+"\n"+a);
					System.out.println("STYLE:"+((DefaultStyledDocument)getDocument()).getLogicalStyle(getDocument().getLength()-1));*/
				} catch (BadLocationException e) {
					System.err.println("Problems appending text:"+e);
				}				
			}
		});
	}
	public void append(String S){
		append(S,null);
	}
	public void appendOutputTextRuns(String s){
		append(s);
	}
	public void copyAsRTF(){
		if (getSelectionStart()==getSelectionEnd())
			return;
		try {
			getToolkit().getSystemClipboard().setContents(new RTFSelection(), null);
		} catch (IllegalStateException e) {
			Toolkit.getDefaultToolkit().beep(); e.printStackTrace();
			return;
		} catch (IOException e) {
			Toolkit.getDefaultToolkit().beep();e.printStackTrace();
		} catch (BadLocationException e) {
			Toolkit.getDefaultToolkit().beep(); e.printStackTrace();
		}
		
	}
	class RTFSelection extends StringSelection{
		private DataFlavor RTF = new DataFlavor("text/rtf", "RTF");
		byte[] data;

		public RTFSelection() throws IOException, BadLocationException {
			super(getSelectedText());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			RTFEditorKit editorKit = new RTFEditorKit();
			editorKit.write(out, getStyledDocument(), getSelectionStart(), getSelectionEnd() );
			data = out.toByteArray();
			out.close();
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] flavors = super.getTransferDataFlavors();
			DataFlavor[] result = Arrays.copyOf(flavors, flavors.length+1);
			result[result.length-1] = RTF;
			return result;
		}
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (flavor.equals(RTF))
				return new ByteArrayInputStream(data);
			else 
				return super.getTransferData(flavor);
		}
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			if (RTF.equals(flavor)) return true;
			else return super.isDataFlavorSupported(flavor);
		}
	}
	
	public String getToolTipText(MouseEvent event){
		
		int P = viewToModel(event.getPoint());
		DefaultStyledDocument doc = ((DefaultStyledDocument)getDocument());
		
		/* System.out.println("--");
		System.out.println(doc.getLogicalStyle(P));
		System.out.println(doc.getLogicalStyle(P).getClass());
		System.out.println(doc.getParagraphElement(P));
		System.out.println(doc.getParagraphElement(P).getAttributes());
		System.out.println(doc.getParagraphElement(P).getAttributes().getResolveParent());
		System.out.println(doc.getParagraphElement(P).getAttributes().getAttribute(AttributeSet.ResolveAttribute));
		System.out.println(doc.getCharacterElement(P));
		System.out.println(doc.getCharacterElement(P).getAttributes());
		System.out.println(doc.getCharacterElement(P).getAttributes().getAttribute(AttributeSet.ResolveAttribute)); */

		// Somehow... we're not getting the logical style :-( 
		String tip = attributesToTooltip(doc.getLogicalStyle(P));
		if (tip != null) return tip;
		else return super.getToolTipText(event);
	}

	public String attributesToTooltip(AttributeSet attributes) {
		return null;
	}
	/** To be used explicitly to override the SmartScroller */
	public void scrollToBottom(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if (isShowing()) {
					int offset = getDocument().getEndPosition().getOffset();
					if (offset>0) offset--; /* OBOB hack */
					setCaretPosition(offset);
					try {
						// If we're in a JScrollPane, force scrolling to bottom and left
						JScrollBar scrollbarV = ((JScrollPane)((JViewport)(getParent())).getParent()).getVerticalScrollBar();
						scrollbarV.setValue(scrollbarV.getMaximum());
						JScrollBar scrollbarH = ((JScrollPane)((JViewport)(getParent())).getParent()).getHorizontalScrollBar();
						scrollbarH.setValue(scrollbarH.getMinimum());
					} catch (Exception e) {/* We're not in a JScrollPane, forget it! */};
				}
			}
		});
	}
	
}

