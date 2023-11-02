from Queue import Queue
from threading import Thread
import time
import traceback

from core.runner.RepairTask import RepairTask
from core.runner.Runner import Runner
from core.renderer.renderer import get_renderer
from config import LOCAL_THREAD


class RunnerWorker(Thread):
    def __init__(self, local_runner, callback):
        Thread.__init__(self)
        self.local_runner = local_runner
        self.callback = callback
        self.daemon = True
        self.pool = RepairThreadPool(LOCAL_THREAD)

    def run(self):
        for task in self.local_runner.tasks:
            self.local_runner.running += [task]
            task.status = "WAITING"
            self.pool.add_repair(task, self.callback)

        self.pool.wait_completion()


class RepairWorker(Thread):
    def __init__(self, tasks):
        Thread.__init__(self)
        self.tasks = tasks
        self.daemon = True
        self.start()

    def run(self):
        while True:
            (task, callback) = self.tasks.get()
            task.status = "STARTED"
            try:
                task.run()
            except Exception, e:
                task.status = "ERROR"
                print e
                traceback.print_exc()
            finally:
                if callback is not None:
                    callback(task)
                self.tasks.task_done()


class RepairThreadPool:
    """Pool of threads consuming tasks from a queue"""
    def __init__(self, num_threads):
        self.tasks = Queue(num_threads)
        self.workers = []
        for _ in range(num_threads):
            self.workers.append(RepairWorker(self.tasks))

    def add_repair(self, task, callback):
        """Add a task to the queue"""
        if task.bug is not None:
            self.tasks.put((task, callback))

    def wait_completion(self):
        """Wait for completion of all the tasks in the queue"""
        self.tasks.join()


class LocalRunner(Runner):

    def __init__(self, tasks, args):
        """
        :type tasks: list of RepairTask
        """
        super(LocalRunner, self).__init__(tasks, args)
        self.tasks = tasks
        self.finished = []
        self.running = []
        self.waiting = []

    def repair_done(self, task):
        if task.status is "STARTED":
            task.status = "DONE"
        task.end_date = time.time()
        self.finished += [task]
        self.running.remove(task)
        pass

    def execute(self):
        worker = RunnerWorker(self, self.repair_done)
        renderer = get_renderer(self)
        worker.start()

        while worker.is_alive():
            if self.is_end_time():
                # kill thread
                pass
            renderer.render()
            time.sleep(1)

        renderer.render_final_result()
