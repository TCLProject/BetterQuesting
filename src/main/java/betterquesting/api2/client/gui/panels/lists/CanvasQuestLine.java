package betterquesting.api2.client.gui.panels.lists;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.enums.EnumQuestState;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuest.RequirementType;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api.questing.IQuestLineEntry;
import betterquesting.api.storage.BQ_Settings;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api2.client.gui.controls.PanelButtonQuest;
import betterquesting.api2.client.gui.misc.GuiRectangle;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.content.PanelGeneric;
import betterquesting.api2.client.gui.panels.content.PanelLine;
import betterquesting.api2.client.gui.panels.content.PanelLine.ShouldDrawPredicate;
import betterquesting.api2.client.gui.resources.colors.GuiColorPulse;
import betterquesting.api2.client.gui.resources.colors.IGuiColor;
import betterquesting.api2.client.gui.resources.lines.IGuiLine;
import betterquesting.api2.client.gui.resources.textures.SimpleTexture;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetLine;
import betterquesting.api2.storage.DBEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import org.lwjgl.input.Keyboard;

import java.util.*;
import java.util.Map.Entry;

/**
 * My class for lazy quest line setup on a scrolling canvas
 */
public class CanvasQuestLine extends CanvasScrolling
{
    private final List<PanelButtonQuest> btnList = new ArrayList<>();
    
    private final int buttonId;
    private IQuestLine lastQL;
    private int zoomToFitMargin = 24;
    
    public CanvasQuestLine(IGuiRect rect, int buttonId)
    {
        super(rect);
        this.setupAdvanceScroll(true, true, 3000);
        this.enableBlocking(false);
        this.buttonId = buttonId;
    }
    
    public Collection<PanelButtonQuest> getQuestButtons()
    {
        return Collections.unmodifiableCollection(this.btnList);
    }
    
    public PanelButtonQuest getButtonAt(int mx, int my)
    {
        float zs = zoomScale.readValue();
        int tx = getTransform().getX();
        int ty = getTransform().getY();
        int smx = (int)((mx - tx) / zs) + lsx;
        int smy = (int)((my - ty) / zs) + lsy;
			
        for(PanelButtonQuest btn : btnList)
        {
            if(btn.rect.contains(smx, smy)) return btn;
        }
        
        return null;
    }
    
    public IQuestLine getQuestLine()
    {
        return lastQL;
    }
    
    public void refreshQuestLine()
    {
        setQuestLine(lastQL);
    }
    
    /**
     * Loads in quests and connecting lines
     * @param line The quest line to load
     */
    public void setQuestLine(IQuestLine line)
    {
        // Rest contents
        this.resetCanvas();
        this.btnList.clear();
        lastQL = line;
        
        if(line == null) return;
        
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        UUID pid = QuestingAPI.getQuestingUUID(player);
        
        String bgString = line.getProperty(NativeProps.BG_IMAGE);
        
        if(!StringUtils.isNullOrEmpty(bgString))
        {
            int bgSize = line.getProperty(NativeProps.BG_SIZE);
            this.addPanel(new PanelGeneric(new GuiRectangle(0, 0, bgSize, bgSize, 1), new SimpleTexture(new ResourceLocation(bgString), new GuiRectangle(0, 0, 256, 256))));
        }
        
        HashMap<Integer, PanelButtonQuest> questBtns = new HashMap<>();
        
        for(DBEntry<IQuestLineEntry> qle : line.getEntries())
        {
            IQuest quest = QuestingAPI.getAPI(ApiReference.QUEST_DB).getValue(qle.getID());
            
            if(!QuestCache.isQuestShown(quest, pid, player)) continue;
            
            GuiRectangle rect = new GuiRectangle(qle.getValue().getPosX(), qle.getValue().getPosY(), qle.getValue().getSizeX(), qle.getValue().getSizeY());
            PanelButtonQuest paBtn = new PanelButtonQuest(rect, buttonId, "", new DBEntry<>(qle.getID(), quest));
            
            this.addPanel(paBtn);
            this.btnList.add(paBtn);
            questBtns.put(qle.getID(), paBtn);
        }
        
        for(Entry<Integer, PanelButtonQuest> entry : questBtns.entrySet())
        {
            DBEntry<IQuest> quest = entry.getValue().getStoredValue();


            //list of required quests to unlock this one, aka parent quests
            List<DBEntry<IQuest>> reqList = QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(quest.getValue().getRequirements());
            
            if(reqList.size() <= 0) continue;
            
            boolean main = quest.getValue().getProperty(NativeProps.MAIN);
            EnumQuestState qState = quest.getValue().getState(player);
            IGuiLine lineRender = null;
            IGuiColor defaultTxLineCol = null;
            
            switch(qState)
            {
                case LOCKED:
                    lineRender = PresetLine.QUEST_LOCKED.getLine();
                    defaultTxLineCol = PresetColor.QUEST_LINE_LOCKED.getColor();
                    break;
                case UNLOCKED:
                    lineRender = PresetLine.QUEST_UNLOCKED.getLine();
                    defaultTxLineCol = PresetColor.QUEST_LINE_UNLOCKED.getColor();
                    break;
                case UNCLAIMED:
                    lineRender = PresetLine.QUEST_PENDING.getLine();
                    defaultTxLineCol = PresetColor.QUEST_LINE_PENDING.getColor();
                    break;
                case COMPLETED:
                    lineRender = PresetLine.QUEST_COMPLETE.getLine();
                    defaultTxLineCol = PresetColor.QUEST_LINE_COMPLETE.getColor();
                    break;
                case REPEATABLE:
                    lineRender = PresetLine.QUEST_REPEATABLE.getLine();
                    defaultTxLineCol = PresetColor.QUEST_LINE_REPEATABLE.getColor();
                    break;
            }

            if(QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(player)){
                List<DBEntry<IQuest>> visReqList = QuestingAPI.getAPI(ApiReference.QUEST_DB).bulkLookup(quest.getValue().getVisRequirements());
                if(reqList.size() <= 0) continue;
                for (DBEntry<IQuest> visReq : visReqList) {
                    PanelButtonQuest parBtn = questBtns.get(visReq.getID());

                    if (parBtn != null) {
                        PanelLine prLine = new PanelLine(parBtn.getTransform(), entry.getValue().getTransform(), PresetLine.QUEST_LOCKED.getLine(), 10, PresetColor.QUEST_LINE_VIS.getColor(), 1);
                        this.addPanel(prLine);
                    }
                }

            }

            if(quest.getValue().showParentConnection()) {
                for(DBEntry<IQuest> req : reqList) {
                    PanelButtonQuest parBtn = questBtns.get(req.getID());

                    if (parBtn != null) {
                        PanelLine prLine = new PanelLine(parBtn.getTransform(), entry.getValue().getTransform(), lineRender, main ? 8 : 4, defaultTxLineCol, 1);
                        this.addPanel(prLine);
                    }
                }
            }
        }
        
        fitToWindow();
    }
    
    public void fitToWindow()
    {
        // Used later to center focus the quest line within the window
        boolean flag = false;
        int minX = 0;
        int minY = 0;
        int maxX = 0;
        int maxY = 0;
        
        for(PanelButtonQuest btn : btnList)
        {
            GuiRectangle rect = btn.rect;
            
            if(!flag)
            {
                minX = rect.getX();
                minY = rect.getY();
                maxX = minX + rect.getWidth();
                maxY = minY + rect.getHeight();
                flag = true;
            } else
            {
                minX = Math.min(minX, rect.getX());
                minY = Math.min(minY, rect.getY());
                maxX = Math.max(maxX, rect.getX() + rect.getWidth());
                maxY = Math.max(maxY, rect.getY() + rect.getHeight());
            }
        }
        
        minX -= zoomToFitMargin;
        minY -= zoomToFitMargin;
        maxX += zoomToFitMargin;
        maxY += zoomToFitMargin;
        
        this.setZoom(Math.min(getTransform().getWidth()/(float)(maxX - minX), getTransform().getHeight()/(float)(maxY - minY)));
        this.refreshScrollBounds();
        
        IGuiRect bounds = getScrollBounds();
        this.setScrollX(bounds.getX() + bounds.getWidth()/2);
        this.setScrollY(bounds.getY() + bounds.getHeight()/2);
        this.updatePanelScroll();
    }
}
