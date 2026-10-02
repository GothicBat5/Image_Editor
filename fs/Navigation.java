import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

//Browser-style navigation history. 
public class Navigation
{
    private final Deque<Path> backStack = new ArrayDeque<>();
    private final Deque<Path> forwardStack = new ArrayDeque<>();
    private Path current;

    public Navigation(Path start)
    {
        this.current = start;
    }

    public Path current() 
    { 
        return current; 
    }

    public void go(Path target)
    {
        if (target.equals(current)) return;
        backStack.push(current);
        forwardStack.clear();
        current = target;
    }

    public boolean canGoBack()    
    { 
        return !backStack.isEmpty(); 
    }
    
    public boolean canGoForward() 
    { 
        return !forwardStack.isEmpty(); 
    }

    public boolean canGoUp()      
    { 
        return current.getParent() != null; 
    }

    public Path peekBack()    
    { 
        return backStack.peek(); 
    }

    public Path peekForward() 
    { 
        return forwardStack.peek(); 
    }

    public Path peekUp()      
    { 
        return current.getParent(); 
    }

    public void goBack()
    {
        if (!canGoBack()) return;
        forwardStack.push(current);
        current = backStack.pop();
    }

    public void goForward()
    {
        if (!canGoForward()) return;
        backStack.push(current);
        current = forwardStack.pop();
    }
}
