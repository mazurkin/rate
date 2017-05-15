EventGate implementation class allows to control count of events per some duration:

```java
public class Some {
    
    private final EventGate gate;
    
    public Some() {
        // allows 1000 events per second
        this.gate = new EventRateGate(1000, 1, TimeUnit.SECOND);
    }
    
    private void handleEvent() {
        if (this.gate.passed()) {
            // process event
        } else {
            // cancel event
        }
    }
}
```
