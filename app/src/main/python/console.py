import sys
import io
import traceback

class ConsoleOutput:
    def __init__(self):
        self.output = io.StringIO()
        self.error = io.StringIO()
        self.original_stdout = sys.stdout
        self.original_stderr = sys.stderr
    
    def __enter__(self):
        sys.stdout = self.output
        sys.stderr = self.error
        return self
    
    def __exit__(self, *args):
        sys.stdout = self.original_stdout
        sys.stderr = self.original_stderr
    
    def get_output(self):
        return self.output.getvalue()
    
    def get_error(self):
        return self.error.getvalue()

def execute(code):
    with ConsoleOutput() as console:
        try:
            exec(code, globals())
            return console.get_output(), console.get_error(), None
        except Exception as e:
            return console.get_output(), console.get_error(), traceback.format_exc()