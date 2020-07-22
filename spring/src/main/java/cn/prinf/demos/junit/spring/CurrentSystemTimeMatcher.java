package cn.prinf.demos.junit.spring;

import org.hamcrest.Description;

public class CurrentSystemTimeMatcher extends BaseMatcher<Long> {
    private final long timeWindow;

    public CurrentSystemTimeMatcher(long timeWindow) {
        this.timeWindow = timeWindow;
    }

    protected boolean matchesSafely(Long time) {
        System.out.println(System.currentTimeMillis());
        if (System.currentTimeMillis() <= (time + timeWindow)) {
            return true;
        } else {
            return false;
        }
    }

    public void describeTo(Description description) {
        description.appendText(" current time not in range " + timeWindow);
    }

    /**
     * @param timeWindow tolerate the gap from System.currentTimeMillis()
     * @return
     */
    public static CurrentSystemTimeMatcher currentTimeEquals(long timeWindow) {
        return new CurrentSystemTimeMatcher(timeWindow);
    }
}
