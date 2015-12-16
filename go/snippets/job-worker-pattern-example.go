/*
一个 golang 实现的任务队列机制, Dispatcher/Worker Pattern (or Job/Worker pattern)
原链接:
    http://marcio.io/2015/07/handling-1-million-requests-per-minute-with-golang/

个人觉得这段代码演示了如何用 golang 创建 Job/Worker 模式的大致实现。虽然代码上感觉有的
地方有问题(详见: dispatch 方法的描述, 对于这段代码中使用 WorkerPool 的地方都需要注意下), 
但仍其应有的参考价值

*/


var (
    MaxWorker = os.Getenv("MAX_WORKERS")
    MaxQueue  = os.Getenv("MAX_QUEUE")
)

// Job represents the job to be run
// 一个需要处理的任务单元
type Job struct {
    Payload Payload //需要上传到亚马逊 s3 服务器上的内容
}

// A buffered channel that we can send work requests on.
var JobQueue chan Job

// Worker represents the worker that executes the job
type Worker struct {
    WorkerPool  chan chan Job //一个任务队列的池子
    JobChannel  chan Job //这个 worker 对应的处理的任务队列
    quit        chan bool //是否退出的标志位
}

//静态方法, 生成一个新的 worker
func NewWorker(workerPool chan chan Job) Worker {
    return Worker{
        WorkerPool: workerPool,
        JobChannel: make(chan Job),
        quit:       make(chan bool)}
}

// Start method starts the run loop for the worker, listening for a quit channel in
// case we need to stop it
// 执行 worker 当队列有任务的时候便一直执行, 直到退出
func (w Worker) Start() {
    go func() {
        for {
            // register the current worker into the worker queue.
            // 将当前 worker 的任务队列注册到 Dispatcher 中
            w.WorkerPool <- w.JobChannel

            select {
            case job := <-w.JobChannel:
                // we have received a work request.
                if err := job.Payload.UploadToS3(); err != nil {
                    log.Errorf("Error uploading to S3: %s", err.Error())
                }

            case <-w.quit:
                // we have received a signal to stop
                return
            }
        }
    }()
}

// Stop signals the worker to stop listening for work requests.
func (w Worker) Stop() {
    go func() {
        w.quit <- true
    }()
}


/*
Dispatcher: 任务分发器, 通过它来向各个 Worker 中分发任务. 它本身是一个 任务队列池
*/
type Dispatcher struct {
    // A pool of workers channels that are registered with the dispatcher
    WorkerPool chan chan Job
}

//创建一个新的 Dispatcher
func NewDispatcher(maxWorkers int) *Dispatcher {
    pool := make(chan chan Job, maxWorkers)
    return &Dispatcher{WorkerPool: pool}
}

func (d *Dispatcher) Run() {
    // starting n number of workers
    for i := 0; i < d.maxWorkers; i++ {
        worker := NewWorker(d.pool)//这个地方应该是 d.WorkerPool
        worker.Start()
    }

    go d.dispatch()
}

//从任务分发的 Main Bus 中取得任务, 分发到各个 worker 中的任务队列中
func (d *Dispatcher) dispatch() {
    for {
        select {
        case job := <-JobQueue:
            // a job request has been received
            go func(job Job) {
                // try to obtain a worker job channel that is available.
                // this will block until a worker is idle
                
                // !! 这个地方我觉得有问题, 每个 woker 创建的时候, chan d.WorkerPool 中才会被放置一条 jobChannel, 
                // 所以这个地方并不会达到一种任务下派的效果，还有一种可能就是多次执行 Worker 的 work 方法。这样会保证 WorkerPool
                // 中不断有数据加入, 但显然这样做有悖于 Worker@work 方法的实现
                
                // 所以这个地方应该是轮询着所有在 WorkerPool 中注册的各个 jobChannel，分别往其中的 jobChannel 放置任务
                // 然后去由 Woker 去执行。所以这个 WorkerPool 最好换成 slice
                jobChannel := <-d.WorkerPool

                // dispatch the job to the worker job channel
                jobChannel <- job
            }(job)
        }
    }
}


//从http中接受处理任务
func payloadHandler(w http.ResponseWriter, r *http.Request) {

  if r.Method != "POST" {
        w.WriteHeader(http.StatusMethodNotAllowed)
        return
    }

  // Read the body into a string for json decoding
    var content = &PayloadCollection{}
    err := json.NewDecoder(io.LimitReader(r.Body, MaxLength)).Decode(&content)
    if err != nil {
        w.Header().Set("Content-Type", "application/json; charset=UTF-8")
        w.WriteHeader(http.StatusBadRequest)
        return
    }

    // Go through each payload and queue items individually to be posted to S3
    for _, payload := range content.Payloads {

        // let's create a job with the payload
        work := Job{Payload: payload}

        // Push the work onto the queue.
                //将任务 push 到队列中, 事件分发的 Main Bus.
        JobQueue <- work
    }

    w.WriteHeader(http.StatusOK)
}

