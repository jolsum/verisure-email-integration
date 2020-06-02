package house.jolsum.central.verisure;

public interface VerisureEventListener {

  void onAlarmArmed();

  void onAlarmDisarmed();

  void onDoorLocked();

  void onDoorUnlocked();
}
