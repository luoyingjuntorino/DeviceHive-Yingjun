from plugin_Initializer import PluginsInitializer, run_initializer
from plugin_updater import SimpleHandler, run_updater
import time 
if __name__ == "__main__":
    time.sleep(90) # sleep 90, to wait all devicehive microservices start, time depend on PC performance
    Initializer = run_initializer() # initializing listener and influx plugins.
    Listener = run_updater() # keep listening if any networkd id added.