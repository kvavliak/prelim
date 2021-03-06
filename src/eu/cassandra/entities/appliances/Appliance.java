package eu.cassandra.entities.appliances;

import eu.cassandra.entities.installations.Installation;
import eu.cassandra.platform.utilities.Constants;
import eu.cassandra.platform.utilities.Params;
import eu.cassandra.platform.utilities.RNG;
import eu.cassandra.platform.utilities.FileUtils;

/**
 * Class modeling an electric appliance. The appliance has a stand by 
 * consumption otherwise there are a number of periods along with their 
 * consumption rates.
 * 
 * @author kyrcha
 * @version prelim
 */
public class Appliance {
	private final int id;
	private final String name;
	private final Installation installation;
	private final float[] consumption;
	private final int[] periods;
	private final int totalCycleTime;
	private final float standByConsumption;
	private final boolean base;
	
	private boolean inUse;
	private long onTick;
	private String who;
	
	public static class Builder {
		private static int idCounter = 0;
		// Required variables
		private final int id;
		private final String name;
		private final Installation installation;
		private final float[] consumption;
		private final int[] periods;
		private final int totalCycleTime;
		private final float standByConsumption;
		private final boolean base;
		// Optional or state related variables
		private long onTick = -1;
		private String who = null;
		public Builder(String aname, Installation ainstallation) {
			id = idCounter++;
			name = aname;
			installation = ainstallation;
			consumption = 
					FileUtils.getFloatArray(Params.APPS_PROPS, name+".power");
			periods = FileUtils.getIntArray(Params.APPS_PROPS, name+".periods");
			int sum = 0;
			for(int i = 0; i < periods.length; i++) {
				sum += periods[i];
			}
			totalCycleTime = sum;
			standByConsumption = 
					FileUtils.getFloat(Params.APPS_PROPS, name+".stand-by");
			base = FileUtils.getBool(Params.APPS_PROPS, name+".base");
		}
		public Appliance build() {
			return new Appliance(this);
		}
	}
	
	private Appliance(Builder builder) {
		id = builder.id;
		name = builder.name;
		installation = builder.installation;
		standByConsumption = builder.standByConsumption;
		consumption = builder.consumption;
		periods = builder.periods;
		totalCycleTime = builder.totalCycleTime;
		base = builder.base;
		inUse = (base) ? true : false;
		onTick = (base) ? RNG.nextInt(Constants.MIN_IN_DAY) : builder.onTick;
		who = builder.who;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Installation getInstallation() {
		return installation;
	}

	public boolean isInUse() {
		return inUse;
	}

	public float getPower(long tick) {
		float power;
		if(isInUse()) {
			long relativeTick = Math.abs(tick - onTick);
			long tickInCycle = relativeTick % totalCycleTime;
			int ticks = 0;
			int periodIndex = 0;
			for(int i = 0; i < periods.length; i++) {
				ticks += periods[i];
				if(tickInCycle < ticks) {
					periodIndex = i;
					break;
				}
			}
			power = consumption[periodIndex];
		} else {
			power = standByConsumption;
		}
		return power;
	}

	public void turnOff() {
		if(!base) {
			inUse = false;
			onTick = -1;
		}
	}

	public void turnOn(long tick, String awho) {
		inUse = true;
		onTick = tick;
		who = awho;
	}

	public long getOnTick() {
		return onTick;
	}
	
	public String getWho() {
		return who;
	}
	
	public static void main(String[] args) {
		Appliance frige = new Appliance.Builder("refrigerator", 
				null).build();
		System.out.println(frige.getId());
		System.out.println(frige.getName());
		Appliance freezer = new Appliance.Builder("freezer", 
				null).build();
		System.out.println(freezer.getId());
		System.out.println(freezer.getName());
		for(int i = 0; i < 100; i++) {
			System.out.println(freezer.getPower(i));
		}
	}
	
}
