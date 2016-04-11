package bq_standard.tasks;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreDummyCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.apache.logging.log4j.Level;
import betterquesting.client.gui.GuiQuesting;
import betterquesting.client.gui.misc.GuiEmbedded;
import betterquesting.quests.QuestDatabase;
import betterquesting.quests.tasks.TaskBase;
import betterquesting.utils.JsonHelper;
import bq_standard.client.gui.editors.GuiScoreEditor;
import bq_standard.client.gui.tasks.GuiTaskScoreboard;
import bq_standard.core.BQ_Standard;
import com.google.gson.JsonObject;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TaskScoreboard extends TaskBase
{
	public String scoreName = "Score";
	public String type = "dummy";
	public int target = 1;
	public ScoreOperation operation = ScoreOperation.MORE_OR_EQUAL;
	
	@Override
	public String getUnlocalisedName()
	{
		return "bq_standard.task.scoreboard";
	}
	
	@Override
	public void Update(EntityPlayer player)
	{
		if(player.ticksExisted%20 == 0 && !QuestDatabase.editMode) // Auto-detect once per second
		{
			Detect(player);
		}
	}
	
	@Override
	public void Detect(EntityPlayer player)
	{
		if(isComplete(player.getUniqueID()))
		{
			return;
		}
		
		Scoreboard board = player.getWorldScoreboard();
		ScoreObjective scoreObj = board == null? null : board.getObjective(scoreName);
		
		if(scoreObj == null)
		{
			try
			{
		        IScoreObjectiveCriteria criteria = (IScoreObjectiveCriteria)IScoreObjectiveCriteria.field_96643_a.get(type);
		        criteria = criteria != null? criteria : new ScoreDummyCriteria(scoreName);
				scoreObj = board.addScoreObjective(scoreName, criteria);
				scoreObj.setDisplayName(scoreName);
			} catch(Exception e)
			{
				BQ_Standard.logger.log(Level.ERROR, "Unable to create score '" + scoreName + "' for task!", e);
			}
		}
		
		Score score = board.func_96529_a(player.getCommandSenderName(), scoreObj);
		int points = score.getScorePoints();
		
		if(operation.checkValues(points, target))
		{
			setCompletion(player.getUniqueID(), true);
		}
	}
	
	@Override
	public void writeToJson(JsonObject json)
	{
		super.writeToJson(json);
		
		json.addProperty("scoreName", scoreName);
		json.addProperty("type", type);
		json.addProperty("target", target);
		json.addProperty("operation", operation.name());
	}
	
	@Override
	public void readFromJson(JsonObject json)
	{
		super.readFromJson(json);
		
		scoreName = JsonHelper.GetString(json, "scoreName", "Score");
		type = JsonHelper.GetString(json, "type", "dummy");
		target = JsonHelper.GetNumber(json, "target", 1).intValue();
		operation = ScoreOperation.valueOf(JsonHelper.GetString(json, "operation", "MORE_OR_EQUAL").toUpperCase());
		operation = operation != null? operation : ScoreOperation.MORE_OR_EQUAL;
	}
	
	public static enum ScoreOperation
	{
		EQUAL("="),
		LESS_THAN("<"),
		MORE_THAN(">"),
		LESS_OR_EQUAL("<="),
		MORE_OR_EQUAL(">=");
		
		String text = "";
		ScoreOperation(String text)
		{
			this.text = text;
		}
		
		public String GetText()
		{
			return text;
		}
		
		public boolean checkValues(int n1, int n2)
		{
			switch(this)
			{
				case EQUAL:
					return n1 == n2;
				case LESS_THAN:
					return n1 < n2;
				case MORE_THAN:
					return n1 > n2;
				case LESS_OR_EQUAL:
					return n1 <= n2;
				case MORE_OR_EQUAL:
					return n1 >= n2;
			}
			
			return false;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiEmbedded getGui(GuiQuesting screen, int posX, int posY, int sizeX, int sizeY)
	{
		return new GuiTaskScoreboard(this, screen, posX, posY, sizeX, sizeY);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen GetEditor(GuiScreen parent, JsonObject data)
	{
		return new GuiScoreEditor(parent, data);
	}
}
