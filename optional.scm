
(import "io")

(define (some x) 
  (list `some x))

(define (none) 
  (list `none))

(define (some? x)    
   (define head (first x))
   (equal? head `some))

(define (none? x)
  (define head (first x))
   (equal? head `none))

(define (optional-unwrap x) 
  (define (print-error) 
    (println "Error: Attempt to unwrap value of none")
    (exit 69))
  (if (some? x) 
    (second x) 
    (print-error)))


