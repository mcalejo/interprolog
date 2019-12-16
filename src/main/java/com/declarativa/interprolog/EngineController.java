/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.declarativa.interprolog.gui.ListenerWindow;


/**
 * An auxiliary object which listens to a PrologEngine and depending on its state will enable/disable a bunch of UI Components and/or Actions.
 * It also provides two actions for pause/continue and abort. Two implementations are provided: one assuming use of timed_call, the other of 
 * (ctrl-C) breaks.
 * @author mc
 *
 */
public class EngineController implements PrologEngineListener{
    HashSet<Object> itemsToDisableWhenBusy = new HashSet<Object>();
    HashSet<Object> itemsToDisableWhenPaused = new HashSet<Object>();
    /** helper objects to make text areas display a hint when empty */
    /** text areas which will have hints and colors affected by the engine state */
    protected HashSet<JTextArea> fields = new HashSet<JTextArea>();
    protected String busyHint = "";
    protected String idleOrPausedHint = "";
    protected String needsMoreInputHint = "";
    protected Color busyColor, idleOrPausedColor, needsMoreInputColor;
    protected HashMap<JTextArea,Color> originalColors = new HashMap<JTextArea,Color>(); // lazily initialized, to get stable colors
    protected HashMap<JTextArea,Color> darkerColors = new HashMap<JTextArea,Color>();
    protected String busyLabel; // for the engine state "label" (button)
    protected String idleOrPausedLabel;
    protected String needsMoreInputLabel;
    public final AbstractAction engineStateAction; // use for display, not input
    public final PauseContinueAction pauseContinueAction;
    public final AbortAction stopAction;
    boolean pauseRequested = false, pauseEnded = false, inPause=false, prologCanWork=true;
    public static final String STOP_MESSAGE = "Query Aborted";
    /** If not null, no timed_call is assumed */
    SubprocessEngine pausableEngine;
    private boolean autoHints;
    
    /**
     * @param pausableEngine optional; if not null will be sent break requests
     * @param autoHints if true query entry fields will have a hint injected according to engine state
     * @see com.xsb.xj.util.XJEngineController
     */
    @SuppressWarnings("serial")
    public EngineController(SubprocessEngine pausableEngine, boolean autoHints){
        this.pausableEngine = pausableEngine;
        this.autoHints=autoHints;
        // This action will oscillate between Pause(disabled/enabled)/Continue:
        pauseContinueAction = new PauseContinueAction();
        // This action will not change name, just enabled state:
        stopAction = new AbortAction();
        engineStateAction = new AbstractAction(){
                @Override
                public void actionPerformed(ActionEvent e) {}		
            }; 
        engineStateAction.putValue(Action.SHORT_DESCRIPTION, "Engine availability state");
    }
    
    @SuppressWarnings("serial")
    class PauseContinueAction extends AbstractAction{
        PauseContinueAction(){
            prepareForPause();
            setEnabled(false);
        }
        public void actionPerformed(ActionEvent e){
            if (getValue(NAME).equals("Pause")){
                setEnabled(false);
                stopAction.setEnabled(false);
                pauseRequested = true; pauseEnded=false;
                if (pausableEngine!=null)
                    pausableEngine.breakEngine();
            } else { // "Continue" clicked
                prepareForPause();
                stopAction.setEnabled(true);
                pauseEnded = true;
                if (pausableEngine!=null)
                    pausableEngine.resumeEngine();
            }
        }
        private void prepareForPause(){
            putValue(NAME,"Pause");
            putValue(Action.SHORT_DESCRIPTION,"Click to pause the engine");
        }
        private void prepareForContinue(){ // called dynamically bellow...
            putValue(NAME,"Continue");
            putValue(Action.SHORT_DESCRIPTION,"Click to resume execution");
        }
        
    }
    @SuppressWarnings("serial")
    class AbortAction extends AbstractAction{
        AbortAction(){
            super("Abort");
            setEnabled(false);
            putValue(Action.SHORT_DESCRIPTION,"Click to abort (end) the query");
        }
        @Override
        public void actionPerformed(ActionEvent e){
            doIt();
        }
        private void doIt(){
            setEnabled(false);
            pauseContinueAction.setEnabled(false);
            pauseContinueAction.prepareForPause();
            prologCanWork = false;
            pauseEnded = true;
            if (pausableEngine!=null){
                if (pausableEngine.isPaused())
                    pausableEngine.abortEngine();
                else 
                    pausableEngine.interrupt();
            }
        }		
    }
    
    void interruptCleanupHack(){
        if (isInPause()){
            pauseEnded = true;
            prologCanWork = false;
        }
    }
    
    /** The user has started a query */
    public void queryStarted(){
        pauseContinueAction.setEnabled(true);
        stopAction.setEnabled(true);
        //System.out.println("queryStarted");
    }
    /** The user query ended */
    public void queryEnded(){
        pauseContinueAction.setEnabled(false);
        stopAction.setEnabled(false);
        //System.out.println("queryEnd");
    }
    
    // PrologEngineListener methods:
    // This will never be called if not using timed_call
    public String willWork(AbstractPrologEngine source){
        //System.out.print("(p)");
        if (pauseRequested) {
            pauseContinueAction.prepareForContinue();
            pauseContinueAction.setEnabled(true);
            if (autoHints) setUItoPausedOrIdle();
        }
        inPause=true;
        if (!source.isAvailable())
            availabilityChanged(source);
        while(pauseRequested & ! pauseEnded) {
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                System.err.println("Anomaly during Pause:"+e);
                break;
            }
        }
        inPause=false;
        if (!source.isAvailable())
            availabilityChanged(source);
        if (pauseRequested) {
            pauseRequested=false;
            if (autoHints) setUItoBusy();
        }
        //System.out.println("prologCanWork:"+prologCanWork+","+this);
        if (prologCanWork) return null;
        prologCanWork = true;
        return STOP_MESSAGE;
    }	
    
    public void setUItoBusy() {
    }
    
    public void setUItoPausedOrIdle() {
    }
    
    public void setUItoNeedsMoreInput() {		
    }
    
    public void javaMessaged(AbstractPrologEngine source){
        // System.out.print("(j)");
    }
    /** In addition to being messaged by the engine, this method also gets called by this controller when entering/leave pause 
     * @param source an engine
     */
    public void availabilityChanged(final AbstractPrologEngine source){
        final boolean inPause = pausableEngine==null ? EngineController.this.inPause: pausableEngine.isPaused();
        final boolean available = source.isAvailable();
        //System.out.println("availabilityChanged:"+available+", inPause:"+inPause);
        SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    if (available) queryEnded();
                    else queryStarted();
                    for (Object item:itemsToDisableWhenBusy){
                        boolean enabled = (available&&!inPause)||(available && inPause && !itemsToDisableWhenPaused.contains(item));
                        enableItem(item,enabled);
                    }
                    for (Object item:itemsToDisableWhenPaused)
                        if (itemsToDisableWhenBusy.contains(item))
                            continue;
                        else enableItem(item,!inPause);
                    if (pausableEngine!=null){
                        // update actions explicitly, as willWork will never execute
                        if (inPause){
                            pauseContinueAction.prepareForContinue();
                            pauseContinueAction.setEnabled(true);
                            stopAction.setEnabled(true);
                            if (autoHints) setUItoPausedOrIdle();
                        } else if (available){
                            pauseContinueAction.prepareForPause();
                            pauseContinueAction.setEnabled(false);
                            stopAction.setEnabled(false);
                            if (autoHints) setUItoPausedOrIdle();
                        } else{
                            pauseContinueAction.prepareForPause();
                            pauseContinueAction.setEnabled(true);
                            stopAction.setEnabled(true);
                            if (autoHints) setUItoBusy();
                        }
                    }
                }
            });
    }
    
    static void enableItem(Object item,boolean enabled){
        if (item instanceof Component){
            ((Component)item).setEnabled(enabled);
            Container top = null;
            if (item instanceof JComponent)
                top = ((JComponent)item).getTopLevelAncestor(); //TODO: NOT working, gets us nulls!
            //System.out.println(item.getComponent());
            if (top!=null){
                if (!(enabled))
                    ListenerWindow.setWaitCursor(top);
                else ListenerWindow.restoreCursor(top);
            }
        } else if (item instanceof Action){
            ((Action)item).setEnabled(enabled);
        }
    }
    
    //TODO: These should probably enable/disable immediately when called
    /** This component or menu item should be enabled only when the engine is available or paused. 
	Its window will get a busy cursor when the engine is busy 
        * @param item some component*/
    public void disableWhenBusy(Component item){
        itemsToDisableWhenBusy.add(item);
    }
    public void disableWhenBusyOrPaused(Component item){
        itemsToDisableWhenPaused.add(item);
        itemsToDisableWhenBusy.add(item);
    }
    public void disableWhenBusyOrPaused(Action item){
        itemsToDisableWhenPaused.add(item);
        itemsToDisableWhenBusy.add(item);
    }
    public void disableWhenPaused(Component item){
        itemsToDisableWhenPaused.add(item);
    }
    public void disableWhenBusy(Action item){
        itemsToDisableWhenBusy.add(item);
    }
    /** Make a new Color to denote "busy" */
    protected static Color makeBusyColorFrom(Color idleOrPaused){
        return idleOrPaused.darker();
    }
    public void setHintsForFields(String busyHint, String idleOrPausedHint, String needsMoreInputHint){
        this.busyHint = busyHint; this.idleOrPausedHint = idleOrPausedHint; this.needsMoreInputHint = needsMoreInputHint;
    }
    public void setColorsForFields(Color busyColor, Color idleOrPausedColor, Color needsMoreInputColor){
        this.busyColor=busyColor; this.idleOrPausedColor=idleOrPausedColor; this.needsMoreInputColor=needsMoreInputColor;
    }
    public void setLabelsForState(String busy,String idleOrPaused,String needsMoreInputLabel){
        this.busyLabel = busy; this.idleOrPausedLabel=idleOrPaused; this.needsMoreInputLabel=needsMoreInputLabel;
    }
    /** For those annoying situations where anonymous classes do not expose their simple methods (void result and no args) elsewhere ;-) 
     * @param target 
     * @param method */
    public static void myMessage(Object target,String method){
        try{target.getClass().getMethod(method).invoke(target);}
        catch(Exception e){throw new RuntimeException(e);}
    }
    
    public boolean isInPause(){
        if (pausableEngine!=null)
            return pausableEngine.isPaused();
        else
            return inPause;
    }
    
    public void stop(){
        stopAction.doIt();
    }
}
