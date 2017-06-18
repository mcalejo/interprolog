package com.declarativa.interprolog.gui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.JTextComponent;

import com.declarativa.interprolog.util.IPException;

/** An up/down key event handler which implements a simple field command history, optionally updating a menu. 
 * Commands are assumed to be strings without any relevant newlines in it - if they exist they're replaced by spaces
 * This is thread unsafe, and assumed to be used in the AWT thread*/

public class CommandHistory extends KeyAdapter {
	/** Command fields that will be listened to to remember their commands */
    HashSet<JTextComponent> fields;
    /** History menus, that will be maintained by this object */
    HashSet<JMenu> menus;
    /** For each menu, optionally stipulates which sole field gets its history items recovered; 
     * null if it broadcasts to all registered fields */
    HashMap<JMenu,JTextComponent> menuFields;
    /** Index in the menu from which history will be displayed, 0 for first */
    /** Indexes of the first items of each menu to be filled by history; previous items will be left untouched */
    HashMap<JMenu,Integer> menuFirstItems;
    ArrayList<String> commands;
    File file;
        
    /** Including other non history (above) items */
    int maxMenuHistoryItems;
    int maxMenuHistoryChars;

    /** index in commands of the next command to be remembered */
    int currentIndex;
        
    public CommandHistory() {
        this(null);
    }
    /** A CommamdHistory which will persist in a text file, as long the application sends us a save() before exiting.
     * On initialization the file is loaded.
     * @param file if null, no persistence
     */
    public CommandHistory(File file) {
        commands = new ArrayList<String>();
        fields = new HashSet<JTextComponent>();
        menus = new HashSet<JMenu>();
        menuFields = new HashMap<JMenu,JTextComponent>();
        menuFirstItems = new HashMap<JMenu,Integer>();
        maxMenuHistoryItems =
            Toolkit.getDefaultToolkit().getScreenSize().height / 30;
        currentIndex = 0;

        if (maxMenuHistoryItems==0) {
            maxMenuHistoryItems = 20;
            System.err.println("Weird screen height: 0!");
        }
        maxMenuHistoryChars = Toolkit.getDefaultToolkit().getScreenSize().width / 20;
        this.file = file;
        if (file!=null)
            load();
    }

    public void clear() {
        commands.clear();
        for (JMenu menu:menus)
        	for (int i=menu.getItemCount()-1; i>=menuFirstItems.get(menu) ; i-- )
        		menu.remove(i);
        currentIndex = 0;
    }
        
    /** Assumes empty history and no registered GUI objects */
    protected void load() {
        if (!file.exists())
            return;
        if (fields.size()>0||menus.size()>0)
            throw new IPException("Inconsistent load");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line = br.readLine())!=null)
                commands.add(line);
            br.close();
            currentIndex = commands.size();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
        
    public void save(){
        if (file==null)
            throw new IPException("CommandHistory has no file");
        try {
            FileWriter fw = new FileWriter(file);
            for (String command:commands){
                String line = command.trim();
                if (line.equals(System.getProperty("line.separator")))
                    continue;
                fw.write(line); fw.write(System.getProperty("line.separator"));
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        
    public void addField(JTextComponent text){
        fields.add(text);
        text.addKeyListener(this);
    }
        
    /** Registers a text field and a menu; text may already be known here, but menu must not
     * @param menu
     * @param firstItem first item to be used for history ( >=0 )
     * @param text
     */
    public void addMenuAndField(JMenu menu,int firstItem,JTextComponent text){
        menus.add(menu);
        if (text != null)
            if (!fields.contains(text))
                addField(text);
        menuFields.put(menu, text);
        menuFirstItems.put(menu, firstItem);
        // copy existing recent history to the new menu:
        int first = (commands.size()<maxMenuHistoryItems ? 0 : commands.size()-maxMenuHistoryItems);
        for (int c=first; c<commands.size(); c++)
            addToMenu(commands.get(c),menu);
    }

    public void keyPressed(KeyEvent e){
        if (e.getKeyCode()==KeyEvent.VK_UP ){ 
            e.consume();
            JTextComponent field = ((JTextComponent)e.getSource());

            if (currentIndex > 0 && currentIndex <= commands.size())
                field.setText(commands.get(--currentIndex));
            else if (currentIndex==0 && commands.size()>0)
                field.setText(commands.get(0));
            else if (commands.size()==0) {
                field.setText("");
                currentIndex = 0;
            }
            //System.out.println("upSz="+commands.size()+"  idx="+currentIndex);
        } else if (e.getKeyCode()==KeyEvent.VK_DOWN){
            e.consume();
            JTextComponent field = ((JTextComponent)e.getSource());

            //System.out.println("dwnSz="+commands.size()+" idx="+currentIndex);
            if (currentIndex >= 0 && currentIndex < commands.size()-1)
                field.setText(commands.get(++currentIndex));
            else if (currentIndex>=commands.size()-1) {
                field.setText("");
                currentIndex = commands.size();
            }
        }
    }
        
    void addToMenu(final String command,JMenu menu){
        String abridged = (command.length()>maxMenuHistoryChars?command.substring(0, maxMenuHistoryChars-1)+"..." : command);
        if (menu.getItemCount()>=maxMenuHistoryItems) 
            menu.remove(menuFirstItems.get(menu));
        JMenuItem item = new JMenuItem(abridged);
        menu.add(item);
        final JTextComponent oneTarget = menuFields.get(menu);
        item.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (oneTarget!=null) 
                        oneTarget.setText(command);
                    else // broadcast to fields all over the GUI:
                        for (JTextComponent field:fields)
                            field.setText(command);
                }
            });
    }
    
    public void addToHistory(String command_){
        String command = command_.replaceAll(System.getProperty("line.separator"), " ");
        if (commands.contains(command))
            forget(command);
        commands.add(command);
        currentIndex = commands.size();
        for (JMenu menu:menus)
            addToMenu(command,menu);
    }

    private void forget(String command) {
        int K = commands.indexOf(command);
        for (JMenu menu:menus){
            int firstVisibleCommand = commands.size() - (menu.getItemCount() - menuFirstItems.get(menu));
            if (K<firstVisibleCommand) continue;
            int delta = commands.size() - menu.getItemCount();
            menu.remove(K-delta-menuFirstItems.get(menu));
        }
        commands.remove(K);
    }

    public String last() {
        if (commands.isEmpty()) return "";
        return commands.get(commands.size()-1);
    }
}
