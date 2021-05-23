# If - Else Statements

Two types of if statements

Block statements via curly braces '{', '}'
```js
if (5 == 5) {
	print(true)
} else if (4 == 6) {
	print('false');
} else {
	print('bad')
}
```

or ternary like version:

```js
func add(x oftype number, y oftype number) {
	return x + y;
}

func something() {
	return 'hi world';
}

func someone() {
	return 'gross';
}

//$ basically a ternary
set x = if 5 == add: something() else: someone();
//$ or
if 5 == add: something();
else: someone();
```