#include <vector>
#include <algorithm>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>


static bool
sortfunc(int dest, int src) {
	return dest < src; 
}

int
main() {
	char	buf[1024];
	while (fgets(buf, sizeof buf, stdin) != NULL) {

		// Sort contractDependencies field in ascending order to avoid non-deterministic response to
		// comments
		if (strstr(buf, "\"contractDependencies\"") != NULL) {
			printf("%s", buf);
			(void) fgets(buf, sizeof buf, stdin);   // the [
			printf("%s", buf);

			std::vector<int> numbers;
			for (;;) {
				if (fgets(buf, sizeof buf, stdin) == NULL) {
					return EXIT_FAILURE;
				}

				char	*numstr = strtok(buf, "\t,");
				if (isdigit((unsigned char)*numstr)) {
					numbers.push_back(strtol(numstr, NULL, 10));
				}

				if (strchr(buf, ']') != NULL) {
					// Finish
					std::sort(numbers.begin(), numbers.end(), sortfunc);
					for (int i = 0; i < numbers.size(); ++i) {
						if (i > 0) printf(", ");
						printf("%d", numbers[i]);
					}
					printf("\n%s\n", buf);
					break;
				}
			}
		} else if (strstr(buf, "\"externalReferences\"") != NULL) {
			// Drop externalReferences completely for now - hopefully this is only used for inline assembly,
			// where the order is non-deterministic which cannot be solved as easily as for the
			// contractDependencies field above
			do {
				if (fgets(buf, sizeof buf, stdin) == NULL) { // at least the [
					return EXIT_FAILURE;
				}
			} while (strchr(buf, ']') == NULL);
		} else {
			printf("%s", buf);
		}
	}
}
