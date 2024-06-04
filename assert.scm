
(define (assert-equal a b) 
	(define (print-error)
		(display a)
		(display " Is not equal to ")
		(display b)
		(newline)
		(exit 1))
		(if (not (equal? a b)) (print-error) null))



