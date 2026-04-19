// Method to retrieve new line handlers
public List<NewlineHandler> getNewlineHandlers() {
    return Arrays.asList(
        new NewlineHandler(Span.obtain(), ...),
        new NewlineHandler(Span.obtain(), ...)
    );
}