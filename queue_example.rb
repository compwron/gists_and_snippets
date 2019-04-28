# Design & implement an API that allows us to offload work into a queue, as well
# as consume and complete jobs on the queue. The API should provide the following
# functionality:
#
# 1) enqueue a job, which would consist of a job type and parameters
#
# 2) register something to handle jobs of a specific type
#    (e.g. this could be a callback or class)
#
# 3) execute all jobs in the queue

class Job2Handler
  def handle(params)
    raise "foo"
    if params.size > 3
      p "larger than 3!"
      {success: true}
    else
      p "nopeeee"
      {success: false, reasons: ["not enough params", "not blue enough"]}
    end
  end
end

class Job
  attr_accessor :job_type, :params
  def initialize(job_type, params)
    @job_type = job_type
    @params = params
  end
end

class JobError
  def initialize(job, error_cause)
    @job = job
    @error_cause = error_cause
    @error_time = Time.now
  end
end

class JobTracker

  def initialize
    @jobs = []
    @handlers = {}
    @failed_jobs = []
  end

  def enqueue(job_type, params)
    @jobs << Job.new(job_type, params)
  end

  def register_handler(job_type, job_handler)
    @handlers[job_type] = job_handler
  end

  def safe_run(&block)
    begin
      block.call
    rescue => e
      p "exception!", e
      {success: false,  reasons: ["threw exception #{e}"]}
    end
  end

  def execute_all
    while @jobs.size > 0
      job = @jobs.pop
      handler = @handlers[job.job_type]
      if handler == nil
        @failed_jobs << JobError.new(job, "no handler")
      else
        handle_result = safe_run { handler.handle(job.params) }
        if !handle_result[:success]
          @failed_jobs << JobError.new(job, handle_result[:reasons])
        end
      end
    end
  end

  def failed_jobs
    p @failed_jobs
  end
end



job_tracker = JobTracker.new
params = ["a", "b", "c"]
job_tracker.enqueue("job type", params)
job_tracker.enqueue("job type 2", [1])
job_tracker.register_handler("job type 2", Job2Handler.new)
job_tracker.register_handler("job type 2", Job2Handler.new)
job_tracker.register_handler("other job type", Job2Handler.new)
job_tracker.execute_all
job_tracker.execute_all
job_tracker.failed_jobs
