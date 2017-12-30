package be.woubuc.wurmunlimited.server.merchantlisting;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Arrays;
import java.util.List;

import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.*;

public class GetListingAction implements ModAction, ActionPerformer, BehaviourProvider {
	
	private final String address;
	private final int port;
	
	private short actionId;
	private ActionEntry actionEntry;
	
	@Override
	public short getActionId() { return actionId; }
	
	/**
	 * Creates the action entry
	 */
	public GetListingAction(String address, int port) {
		this.address = address;
		this.port = port;
		
		actionId = (short) ModActions.getNextActionId();
		actionEntry = new ActionEntryBuilder(actionId,
				"Get listing",
				"getting listing",
				new int[] {
						0,  // ACTION_TYPE_QUICK
						6,  // ACTION_TYPE_NOMOVE
						48, // ACTION_TYPE_ENEMY_ALWAYS
						37  // ACTION_TYPE_NEVER_USE_ACTIVE_ITEM
				}).build();
		
		ModActions.registerAction(actionEntry);
	}
	
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
		if (performer instanceof Player && target.isNpcTrader()) {
			return Arrays.asList(actionEntry);
		}
		
		return null;
	}
	
	@Override
	public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
		if (performer instanceof Player && target.isNpcTrader()) {
			String hashid = MerchantListingMod.hashids.encode(target.getWurmId());
			performer.getCommunicator().sendNormalServerMessage(
					port == 80 ? String.format("URL: %s/%s", address, hashid)
							   : String.format("URL: %s:%s/%s", address, port, hashid)
			);
			
			return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
		}
		
		return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
	}

}
