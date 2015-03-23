 	function BookListCtrl($scope, $http, $templateCache) {
 		$scope.listBooks = function() {
 			$http({method: 'GET', url: './api/book'}).
  				success(function(data, status, headers, config) {
    		    	$scope.books = data;                  //set view model
    		    	$scope.view = './Partials/list.html'; //set to list view
  				}).
  				error(function(data, status, headers, config) {
  					$scope.books = data || "Request failed";
  					$scope.status = status;
  					$scope.view = './Partials/list.html';
  				});
  		}
  			
  		$scope.showBook = function(id) {
  			$http({method: 'GET', url: './api/book/' + id}).
  				success(function(data, status, headers, config) {
  					$scope.bookDetail = data;               //set view model
  					$scope.view = './Partials/detail.html'; //set to detail view
  				}).
  				error(function(data, status, headers, config) {
  					$scope.bookDetail = data || "Request failed";
  					$scope.status = status;
  					$scope.view = './Partials/detail.html';
  				});
  		}

  		$scope.updateBook = function(book) {
  		    $http({method: 'PUT', url: './api/book/' + book.isbn, data: book    }).
  		        success(function(data,status,headers,config) {
  					$scope.bookDetail = data;               //set view model
  					$scope.view = './Partials/detail.html'; //set to detail view
  		        }).
  		        error(function(data,status,headers,config) {
  					$scope.bookDetail = data || "Request failed";
  					$scope.status = status;
  					$scope.view = './Partials/detail.html';
  		        });
  		}

        $scope.addBook = function(book) {
            $http({method: 'POST', url: './api/book/', data: book    }).
                success(function(data,status,headers,config) {
                    $scope.bookDetail = data;               //set view model
                    $scope.view = './Partials/detail.html'; //set to detail view
                }).
                error(function(data,status,headers,config) {
                    $scope.bookDetail = data || "Request failed";
                    $scope.status = status;
                    $scope.view = './Partials/detail.html';
                });
        }
  		
  		$scope.view = './Partials//list.html'; //set default view
  		$scope.listBooks();
 	}
 	BookListCtrl.$inject = ['$scope', '$http', '$templateCache'];