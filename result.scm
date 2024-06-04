
(define (ok x) 
  (list 'ok x))

(define (err e)
  (list 'err e))

(define (ok? x) 
  (define head (first x))
  (equal? head 'ok))

(define (err? x) 
  (define head (first x))
  (equal? head 'err))

(define (result-unwrap x) 
  (define (print-error msg) 
    (println "Error: Attempt to unwrap value of err")
    (exit 69))
  (if (ok? x) 
    (second x) 
    (print-error (second x))))


