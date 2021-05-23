# Function Syntax

use the 'func' keyword before any function name

```js

// z is a default parameter, meaning that if no arg passed into z it will default to 5
func funStuff(x, y, z = 5) {
	return x + y + z;

	// below wont work
	print(x - y);
}

func coolStuff() {
	return 5;
}

```

# Function Call Syntax

```js
print(funStuff('5', 4));
// prints 14

// assign function call to variable (whatever the function returns)
set returnVal = funStuff(4, 3, 6);
print(returnVal);

// assign function to variable
set funcvar;
funcvar = funStuff;

// or
set funcvar = funStuff;
```